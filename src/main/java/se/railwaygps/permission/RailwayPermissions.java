package se.railwaygps.permission;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import se.railwaygps.RailwayGps;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Optional permissions-provider bridge with a strict op-only fallback. */
public final class RailwayPermissions {
	private static final Provider PROVIDER = discoverProvider();

	private RailwayPermissions() { }

	public static boolean has(ServerPlayer player, RailwayPermission permission) {
		return has(player.createCommandSourceStack(), permission);
	}

	public static boolean has(CommandSourceStack source, RailwayPermission permission) {
		boolean operator = source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
		if (!(source.getEntity() instanceof ServerPlayer player)) return operator;
		return PermissionPolicy.authorize(PROVIDER.query(player, permission.node()), operator);
	}

	public static RailwayCapabilities capabilities(ServerPlayer player) {
		return RailwayCapabilities.resolve(PROVIDER.present(), permission -> has(player, permission));
	}

	public static boolean providerPresent() {
		return PROVIDER.present();
	}

	private static Provider discoverProvider() {
		try {
			Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
			Method get = provider.getMethod("get");
			RailwayGps.LOGGER.info("Using LuckPerms for scoped Railway GPS authorization");
			return new Provider() {
				@Override public Optional<Boolean> query(ServerPlayer player, String node) {
					try {
						Object api = get.invoke(null);
						Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
						Object user = userManager.getClass().getMethod("getUser", UUID.class)
								.invoke(userManager, player.getUUID());
						if (user == null) return Optional.of(false);
						Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
						Object permissionData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
						Object result = permissionData.getClass().getMethod("checkPermission", String.class)
								.invoke(permissionData, node);
						return Optional.of((boolean) result.getClass().getMethod("asBoolean").invoke(result));
					} catch (ReflectiveOperationException | RuntimeException exception) {
						RailwayGps.LOGGER.warn("LuckPerms failed for {}; denying access", node, exception);
						return Optional.of(false);
					}
				}
				@Override public boolean present() { return true; }
			};
		} catch (ClassNotFoundException | NoSuchMethodException ignored) {
			// Try the provider-neutral Fabric permissions bridge next.
		}
		try {
			Class<?> api = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
			Method check = api.getMethod("check", CommandSourceStack.class, String.class);
			RailwayGps.LOGGER.info("Using Fabric Permissions API for scoped Railway GPS authorization");
			return new Provider() {
				@Override public Optional<Boolean> query(ServerPlayer player, String node) {
					try {
						return Optional.of((boolean) check.invoke(null, player.createCommandSourceStack(), node));
					} catch (ReflectiveOperationException | RuntimeException exception) {
						RailwayGps.LOGGER.warn("Permission provider failed for {}; denying access", node, exception);
						return Optional.of(false);
					}
				}
				@Override public boolean present() { return true; }
			};
		} catch (ClassNotFoundException | NoSuchMethodException ignored) {
			RailwayGps.LOGGER.info("No compatible permissions provider found; using op-only Railway GPS tools");
			return Provider.NONE;
		}
	}

	private interface Provider {
		Provider NONE = new Provider() {
			@Override public Optional<Boolean> query(ServerPlayer player, String node) { return Optional.empty(); }
			@Override public boolean present() { return false; }
		};
		Optional<Boolean> query(ServerPlayer player, String node);
		boolean present();
	}
}

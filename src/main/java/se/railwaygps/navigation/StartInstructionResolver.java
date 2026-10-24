package se.railwaygps.navigation;

import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.RailwayNetworkValidator;
import se.railwaygps.railway.model.TrackDirection;

import java.util.Comparator;

/** Resolves the first instruction from physical rails, never from the player's camera. */
public final class StartInstructionResolver {
	private static final double CURRENT_RAIL_DISTANCE = 0.85;
	private static final double ZONE_RAIL_DISTANCE = 6.0;

	private StartInstructionResolver() {
	}

	public static PhysicalInstructionResolver.Instruction resolve(
			RailwayNetwork network, PhysicalRouteLeg outgoing, double x, double y, double z) {
		PolylineMatcher.Match outgoingMatch = PolylineMatcher.nearest(outgoing.points(), x, y, z).orElse(null);
		if (outgoingMatch != null && outgoingMatch.distanceToTrack() <= CURRENT_RAIL_DISTANCE) {
			return new PhysicalInstructionResolver.Instruction(PhysicalInstructionResolver.Direction.STRAIGHT, null);
		}

		return network.tracks().values().stream()
				.filter(track -> track.status() == PhysicalTrack.Status.VERIFIED)
				.flatMap(track -> track.directions().stream().map(direction -> new PhysicalRouteLeg(track, direction)))
				.filter(leg -> RailwayNetworkValidator.endPoint(leg.track(), leg.direction()).equals(outgoing.from()))
				.map(leg -> new Candidate(leg, PolylineMatcher.nearest(leg.points(), x, y, z).orElse(null)))
				.filter(candidate -> candidate.match != null && candidate.match.distanceToTrack() <= ZONE_RAIL_DISTANCE)
				.min(Comparator.comparingDouble(candidate -> candidate.match.distanceToTrack()))
				.map(candidate -> PhysicalInstructionResolver.between(network, candidate.leg, outgoing))
				.orElseGet(() -> new PhysicalInstructionResolver.Instruction(
						PhysicalInstructionResolver.Direction.UNKNOWN, null));
	}

	private record Candidate(PhysicalRouteLeg leg, PolylineMatcher.Match match) {
	}
}

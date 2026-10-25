package se.railwaygps.navigation;

import se.railwaygps.railway.model.TrackDirection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ActiveRouteTracks {
	private ActiveRouteTracks() { }

	public static Map<String, TrackDirection> remaining(Optional<PhysicalRoutePlan> plan, int currentLeg) {
		Map<String, TrackDirection> result = new LinkedHashMap<>();
		plan.ifPresent(value -> {
			for (int index = Math.max(0, currentLeg); index < value.legs().size(); index++) {
				result.put(value.legs().get(index).track().id(), value.legs().get(index).direction());
			}
		});
		return Map.copyOf(result);
	}
}

package se.railwaygps.navigation;

import se.railwaygps.model.Route;

public record RouteLeg(Route route, String from, String to, boolean reversed) {
}

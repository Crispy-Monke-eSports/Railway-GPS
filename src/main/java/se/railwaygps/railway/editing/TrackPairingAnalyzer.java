package se.railwaygps.railway.editing;

import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Finds distinct opposite-direction rails that are geometrically parallel. */
public final class TrackPairingAnalyzer {
	private static final int SAMPLE_COUNT = 17;
	private static final double MIN_HORIZONTAL_SPACING = 0.75;
	private static final double MAX_ENDPOINT_SPACING = 6.0;
	private static final double MAX_SAMPLE_SPACING = 8.0;
	private static final double MAX_AVERAGE_SPACING = 6.0;
	private static final double MAX_VERTICAL_SPACING = 1.5;
	private static final double MAX_SPACING_DEVIATION = 2.5;
	private static final double MIN_ALIGNMENT = 0.82;
	private static final double MAX_LENGTH_RATIO = 1.35;

	private TrackPairingAnalyzer() { }

	public static List<Proposal> proposals(RailwayNetwork network) {
		List<Proposal> raw = new ArrayList<>();
		List<PhysicalTrack> tracks = network.tracks().values().stream()
				.filter(track -> track.status() == PhysicalTrack.Status.VERIFIED && track.pairedTrackId() == null)
				.toList();
		for (int first = 0; first < tracks.size(); first++) {
			for (int second = first + 1; second < tracks.size(); second++) {
				analyze(tracks.get(first), tracks.get(second)).ifPresent(raw::add);
			}
		}
		Map<String, Integer> candidateCounts = new HashMap<>();
		raw.forEach(proposal -> {
			candidateCounts.merge(proposal.firstTrackId(), 1, Integer::sum);
			candidateCounts.merge(proposal.secondTrackId(), 1, Integer::sum);
		});
		return raw.stream().map(proposal -> proposal.withAmbiguous(
				candidateCounts.getOrDefault(proposal.firstTrackId(), 0) > 1
						|| candidateCounts.getOrDefault(proposal.secondTrackId(), 0) > 1))
				.filter(proposal -> !network.rejectedTrackPairings().contains(
						pairKey(proposal.firstTrackId(), proposal.secondTrackId())))
				.sorted((first, second) -> Double.compare(second.quality(), first.quality()))
				.toList();
	}

	public static String pairKey(String firstTrackId, String secondTrackId) {
		return firstTrackId.compareTo(secondTrackId) <= 0
				? firstTrackId + "|" + secondTrackId : secondTrackId + "|" + firstTrackId;
	}

	public static Optional<Proposal> analyze(PhysicalTrack first, PhysicalTrack second) {
		if (first.id().equals(second.id()) || first.pairedTrackId() != null || second.pairedTrackId() != null
				|| first.status() != PhysicalTrack.Status.VERIFIED || second.status() != PhysicalTrack.Status.VERIFIED
				|| !first.sectionId().equals(second.sectionId()) || !first.dimension().equals(second.dimension())
				|| !first.fromControlPoint().equals(second.toControlPoint())
				|| !first.toControlPoint().equals(second.fromControlPoint())
				|| first.points().size() < 2 || second.points().size() < 2) {
			return Optional.empty();
		}

		List<TrackPoint> alignedSecond = second.points().reversed();
		double lengthRatio = Math.max(first.length(), second.length()) / Math.min(first.length(), second.length());
		if (!Double.isFinite(lengthRatio) || lengthRatio > MAX_LENGTH_RATIO) return Optional.empty();

		double spacingSum = 0;
		double spacingSquaredSum = 0;
		double maximumSpacing = 0;
		double verticalSum = 0;
		double horizontalSum = 0;
		double alignmentSum = 0;
		Point previousFirst = null;
		Point previousSecond = null;
		for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
			double fraction = sample / (double) (SAMPLE_COUNT - 1);
			Point a = sample(first.points(), fraction);
			Point b = sample(alignedSecond, fraction);
			double horizontal = Math.hypot(a.x - b.x, a.z - b.z);
			double vertical = Math.abs(a.y - b.y);
			double spacing = distance(a, b);
			if (horizontal < MIN_HORIZONTAL_SPACING || vertical > MAX_VERTICAL_SPACING
					|| spacing > MAX_SAMPLE_SPACING) return Optional.empty();
			if ((sample == 0 || sample == SAMPLE_COUNT - 1) && spacing > MAX_ENDPOINT_SPACING) return Optional.empty();
			spacingSum += spacing;
			spacingSquaredSum += spacing * spacing;
			maximumSpacing = Math.max(maximumSpacing, spacing);
			verticalSum += vertical;
			horizontalSum += horizontal;
			if (previousFirst != null) alignmentSum += alignment(previousFirst, a, previousSecond, b);
			previousFirst = a;
			previousSecond = b;
		}
		double averageSpacing = spacingSum / SAMPLE_COUNT;
		double deviation = Math.sqrt(Math.max(0, spacingSquaredSum / SAMPLE_COUNT - averageSpacing * averageSpacing));
		double alignment = alignmentSum / (SAMPLE_COUNT - 1);
		if (averageSpacing > MAX_AVERAGE_SPACING || deviation > MAX_SPACING_DEVIATION || alignment < MIN_ALIGNMENT) {
			return Optional.empty();
		}

		double spacingQuality = 1 - Math.min(1, deviation / MAX_SPACING_DEVIATION);
		double distanceQuality = 1 - Math.min(1, Math.abs(averageSpacing - 2.0) / MAX_AVERAGE_SPACING);
		double quality = clamp(0.50 * alignment + 0.30 * spacingQuality + 0.20 * distanceQuality);
		return Optional.of(new Proposal(first.id(), second.id(), averageSpacing, maximumSpacing,
				verticalSum / SAMPLE_COUNT, horizontalSum / SAMPLE_COUNT, alignment, quality, false));
	}

	private static Point sample(List<TrackPoint> points, double fraction) {
		double total = 0;
		for (int index = 1; index < points.size(); index++) total += distance(points.get(index - 1), points.get(index));
		double target = total * fraction;
		double traversed = 0;
		for (int index = 1; index < points.size(); index++) {
			TrackPoint first = points.get(index - 1);
			TrackPoint second = points.get(index);
			double segment = distance(first, second);
			if (traversed + segment >= target || index == points.size() - 1) {
				double t = segment == 0 ? 0 : (target - traversed) / segment;
				t = Math.max(0, Math.min(1, t));
				return new Point(first.x() + (second.x() - first.x()) * t,
						first.y() + (second.y() - first.y()) * t,
						first.z() + (second.z() - first.z()) * t);
			}
			traversed += segment;
		}
		TrackPoint last = points.getLast();
		return new Point(last.x(), last.y(), last.z());
	}

	private static double alignment(Point a1, Point a2, Point b1, Point b2) {
		double ax = a2.x - a1.x;
		double ay = a2.y - a1.y;
		double az = a2.z - a1.z;
		double bx = b2.x - b1.x;
		double by = b2.y - b1.y;
		double bz = b2.z - b1.z;
		double lengths = Math.sqrt(ax * ax + ay * ay + az * az) * Math.sqrt(bx * bx + by * by + bz * bz);
		return lengths < 0.0001 ? 0 : (ax * bx + ay * by + az * bz) / lengths;
	}

	private static double distance(TrackPoint first, TrackPoint second) {
		return Math.sqrt(square(first.x() - second.x()) + square(first.y() - second.y())
				+ square(first.z() - second.z()));
	}

	private static double distance(Point first, Point second) {
		return Math.sqrt(square(first.x - second.x) + square(first.y - second.y) + square(first.z - second.z));
	}

	private static double square(double value) { return value * value; }
	private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }

	public record Proposal(String firstTrackId, String secondTrackId, double averageSpacing, double maximumSpacing,
			double averageVerticalSpacing, double averageHorizontalSpacing, double alignment, double quality,
			boolean ambiguous) {
		private Proposal withAmbiguous(boolean value) {
			return new Proposal(firstTrackId, secondTrackId, averageSpacing, maximumSpacing,
					averageVerticalSpacing, averageHorizontalSpacing, alignment, quality, value);
		}
	}

	private record Point(double x, double y, double z) { }
}

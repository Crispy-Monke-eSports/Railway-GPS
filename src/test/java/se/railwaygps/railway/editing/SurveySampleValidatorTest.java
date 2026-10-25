package se.railwaygps.railway.editing;

import net.minecraft.world.level.block.state.properties.RailShape;
import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurveySampleValidatorTest {
	@Test
	void acceptsStationarySamplesStraightRailsAndCurves() {
		SurveySampleValidator validator = new SurveySampleValidator();
		validator.observeRail(point(0, 64, 0), RailShape.EAST_WEST);
		validator.observeRail(point(0, 64, 0), RailShape.EAST_WEST);
		validator.observeRail(point(1, 64, 0), RailShape.SOUTH_WEST);
		validator.observeRail(point(1, 64, 1), RailShape.NORTH_SOUTH);

		assertNull(validator.failure());
		assertEquals(3, validator.points().size());
	}

	@Test
	void acceptsAConnectedSlopeButRejectsAHeightJump() {
		assertTrue(SurveySampleValidator.connected(point(0, 64, 0), RailShape.ASCENDING_EAST,
				point(1, 65, 0), RailShape.EAST_WEST));
		assertFalse(SurveySampleValidator.connected(point(0, 64, 0), RailShape.EAST_WEST,
				point(1, 65, 0), RailShape.EAST_WEST));
		assertFalse(SurveySampleValidator.connected(point(0, 64, 0), RailShape.ASCENDING_EAST,
				point(1, 66, 0), RailShape.EAST_WEST));
	}

	@Test
	void rejectsGapsAndLateralJumpsToAdjacentParallelRail() {
		assertFalse(SurveySampleValidator.connected(point(0, 64, 0), RailShape.EAST_WEST,
				point(2, 64, 0), RailShape.EAST_WEST));
		assertFalse(SurveySampleValidator.connected(point(0, 64, 0), RailShape.EAST_WEST,
				point(0, 64, 1), RailShape.EAST_WEST));
	}

	@Test
	void toleratesOneMissingSampleButRejectsPersistentRailLoss() {
		SurveySampleValidator validator = new SurveySampleValidator();
		validator.observeRail(point(0, 64, 0), RailShape.EAST_WEST);
		validator.observeMissingRail();
		validator.observeRail(point(1, 64, 0), RailShape.EAST_WEST);
		assertNull(validator.failure());

		validator.observeMissingRail();
		validator.observeMissingRail();
		validator.observeMissingRail();
		assertEquals(SurveySampleValidator.Reason.RAIL_LOST, validator.failure().reason());
	}

	@Test
	void teleportIsAlwaysBlocking() {
		SurveySampleValidator validator = new SurveySampleValidator();
		validator.observeRail(point(0, 64, 0), RailShape.EAST_WEST);
		validator.observeTeleport();
		validator.observeRail(point(1, 64, 0), RailShape.EAST_WEST);

		assertEquals(SurveySampleValidator.Reason.POSITION_JUMP, validator.failure().reason());
		assertEquals(1, validator.points().size());
	}

	private static TrackPoint point(int x, int y, int z) { return new TrackPoint(x, y, z); }
}

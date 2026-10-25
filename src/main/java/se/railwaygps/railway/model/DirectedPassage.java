package se.railwaygps.railway.model;

/** One independently verified traversal over shared physical geometry. */
public record DirectedPassage(TraversalRef traversal, String startPortId, String endPortId, double length) { }

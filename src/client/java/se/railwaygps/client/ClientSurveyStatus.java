package se.railwaygps.client;

import se.railwaygps.network.SurveyStatusPayload;

/** Last server-authoritative survey state received by this client. */
public final class ClientSurveyStatus {
	private static SurveyStatusPayload status = idle();

	private ClientSurveyStatus() { }

	public static SurveyStatusPayload get() { return status; }
	public static void replace(SurveyStatusPayload next) { status = next; }
	public static void clear() { status = idle(); }

	private static SurveyStatusPayload idle() {
		return new SurveyStatusPayload("idle", "railwaygps.survey.status.idle", "", 0, 0, 0, 0);
	}
}

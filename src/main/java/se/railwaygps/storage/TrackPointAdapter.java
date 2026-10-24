package se.railwaygps.storage;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import se.railwaygps.model.TrackPoint;

import java.io.IOException;

final class TrackPointAdapter extends TypeAdapter<TrackPoint> {
	@Override
	public void write(JsonWriter out, TrackPoint point) throws IOException {
		if (point == null) {
			out.nullValue();
			return;
		}
		out.beginArray();
		out.value(point.x());
		out.value(point.y());
		out.value(point.z());
		out.endArray();
	}

	@Override
	public TrackPoint read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		in.beginArray();
		int x = in.nextInt();
		int y = in.nextInt();
		int z = in.nextInt();
		if (in.hasNext()) {
			throw new IOException("Track point must contain exactly three coordinates");
		}
		in.endArray();
		return new TrackPoint(x, y, z);
	}
}

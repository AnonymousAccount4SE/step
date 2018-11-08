package step.plugins.datatable.formatters;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;

import org.bson.BSONException;
import org.bson.Document;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;
import org.bson.json.StrictCharacterStreamJsonWriter;
import org.bson.types.ObjectId;

public class DocumentToJsonFormatter {

	/**
	 * We using a custom JsonWriter in order to match to the JSON format used by the JacksonMapperProvider
	 * The main differences between the mapper of JacksonMapperProvider and the default implementation of JsonWriter:
	 * - ObjectId: the JacksonMapperProvider uses a custom serializer for ObjectId which serializes ObjectId as string.
	 * - Field name for "_id": the default implementation of JsonWriter calls the ID field "_id" while the mapper of JacksonMapperProvider calls it "id"
	 * - Long values are serialized {"$long":"13540"} by the default JsonWriter while the mapper of JacksonMapperProvider writes "13540" 
	 */
	public static class CustomJsonWriter extends JsonWriter {

		private Writer writer;
		
		public CustomJsonWriter(Writer writer) {
			super(writer);
			this.writer = writer;
		}

		public CustomJsonWriter(Writer writer, JsonWriterSettings settings) {
			super(writer, settings);
			this.writer = writer;
		}

		@Override
		public void doWriteObjectId(ObjectId objectId) {
			// write ObjectId as string
			writeString(objectId.toString());
		}
		
		@Override
		public void writeName(String name) {
			// changes the name of the field _id to id
			if("_id".equals(name)) {
				super.writeName("id");
			} else {
				super.writeName(name);
			}
		}

		@Override
		protected void doWriteInt64(long value) {
			// writing Int64 as string instead of {"numberLong":"1111"}
			Field f;
			try {
				// we have to use reflection to access the private member strictJsonWriter
				f = this.getClass().getSuperclass().getDeclaredField("strictJsonWriter");
				f.setAccessible(true);
				StrictCharacterStreamJsonWriter writer = (StrictCharacterStreamJsonWriter) f.get(this);
				writer.writeNumber(Long.toString(value));
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new BSONException("Wrapping IOException", e);
			}
		}
	}
	
	public String format(Document row) {
		// Code from Document.toJson()
		JsonWriter writer = new CustomJsonWriter(new StringWriter(), new JsonWriterSettings());
		new DocumentCodec().encode(writer, row, EncoderContext.builder().isEncodingCollectibleDocument(true).build());
        String json =  writer.getWriter().toString();
		return json;
	}
}


// for basic struct offsets ...

/*

typename {
field1
field2
field3
}

converts to:
abstract class typenameAbstract {
public type getfield1() {
  bb.getBLAH(offset);
}
}

 */

#include <stdio.h>

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>

// the one that has the generic native support methods on it
char *rootClass = "AVNative";

enum ctype {
	INT8,
	INT16,
	INT32,
	INT64,
	PTR,
	PTR_INDEX,
	ENUM,
	INT32_ARRAY,
	// only generate an 'offset' variable for more complex stuff
	OFFSET
};

struct field {
	char *name;
	char *jtype;
	enum ctype ctype;
	int flags;
	int offset;
	int size; // for structs/pointers
	char *sizeStr; // alternative for structs/pointers
};

// getter
#define G 1
// setter
#define S 2
// public
#define P 4
#define GS 3
#define PG (P|G)
#define PS (P|S)
#define PGS (P|S|G)

struct type {
	char *name;
	int nfields;
	struct field *fields;
};

#define LEN(x) (sizeof(x) / sizeof(x[0]))
#define OFF(type, field) ((int)(long)(&((type *)NULL)->field))

struct field avcodec_context_fields[] = {
	{ "Width", "int", INT32, PGS, OFF(AVCodecContext, width) },
	{ "Height", "int", INT32, PGS, OFF(AVCodecContext, height) },
	{ "PixFmt", "PixelFormat", ENUM, PG, OFF(AVCodecContext, pix_fmt), 1 },
	{ "CodecType", "int", INT32, PG, OFF(AVCodecContext, codec_type) },
	{ "CodecID", "int", INT32, PG, OFF(AVCodecContext, codec_id) },
};

struct field avformat_context_fields[] = {
	{ "NBStreams", "int", INT32, PG, OFF(AVFormatContext, nb_streams) },
	{ "Stream", "AVStream", PTR_INDEX, PG, OFF(AVFormatContext, streams[0]), sizeof(AVStream) },
};

struct field avpacket_fields[] = {
	{ "PTS", "long", INT64, PG, OFF(AVPacket, pts) },
	{ "DTS", "long", INT64, PG, OFF(AVPacket, dts) },
	{ "Size", "int", INT32, PG, OFF(AVPacket, size) },
	{ "StreamIndex", "int", INT32, PG, OFF(AVPacket, stream_index) },
};

struct field avframe_fields[] = {
	{ "LineSize", "int", INT32_ARRAY, PG, OFF(AVFrame, linesize) },
	{ "DataOffset", "int", OFFSET, G, OFF(AVFrame, data) },
};

struct field avstream_fields[] = {
	{ "Index", "int", INT32, PG, OFF(AVStream, index) },
	{ "Codec", "AVCodecContext", PTR, PG, OFF(AVStream, codec), sizeof(AVCodecContext) },
};

struct type types[] = {
	{ "AVCodecContext", LEN(avcodec_context_fields), avcodec_context_fields },
	{ "AVFormatContext", LEN(avformat_context_fields), avformat_context_fields },
	{ "AVPacket", LEN(avpacket_fields), avpacket_fields },
	{ "AVStream", LEN(avstream_fields), avstream_fields },
	{ "AVFrame", LEN(avframe_fields), avframe_fields },
};
int ntypes = LEN(types);

int main(int argc, char **argv) {
	int i;
	char fname[1024];
	int size = sizeof(void *) * 8;

	// dump concrete class
	for (i=0;i<ntypes;i++) {
		struct type *t = &types[i];

		sprintf(fname, "%s%d.java", t->name, size);
		FILE *out = fopen(fname, "w");

		fprintf(out, "// Auto-generated, editing would be pointless\n");
		fprintf(out, "package au.notzed.jjmpeg;\n"
			"import java.nio.ByteBuffer;\n"
			);
		fprintf(out, "class %s%d extends %s {\n", t->name, size, t->name);
		fprintf(out,
			"\t%s%d(ByteBuffer p) {\n"
			"\t\tsuper(p);\n"
			"\t}\n"
			, t->name, size
			);

		int nfields = t->nfields;
		int j;

		for (j=0;j<nfields;j++) {
			struct field *f = &t->fields[j];

			if (f->flags & G) {
				fprintf(out, "\t");
				if (f->flags & P)
					fprintf(out, "public ");

				fprintf(out, "%s ", f->jtype);

				if (f->ctype == PTR_INDEX) {
					fprintf(out, "get%sAt(int i) {\n", f->name);
				} else if (f->ctype == INT32_ARRAY) {
					fprintf(out, "get%sAt(int i) {\n", f->name);
				} else {
					fprintf(out, "get%s() {\n", f->name);
				}

				switch (f->ctype) {
				case INT8:
					fprintf(out, "\t\treturn p.getByte(%d);\n", f->offset);
					break;
				case INT16:
					fprintf(out, "\t\treturn p.getShort(%d);\n", f->offset);
					break;
				case INT32:
					fprintf(out, "\t\treturn p.getInt(%d);\n", f->offset);
					break;
				case INT64:
					fprintf(out, "\t\treturn p.getLong(%d);\n", f->offset);
					break;
				case PTR:
					fprintf(out, "\t\treturn %s.create(%s.getPointer(p, %d, %d));\n", f->jtype, rootClass, f->offset, f->size);
					break;
				case PTR_INDEX:
					if (f->sizeStr == NULL)
						fprintf(out, "\t\treturn %s.create(%s.getPointerIndex(p, %d, %d, i));\n", f->jtype, rootClass, f->offset, f->size);
					else
						fprintf(out, "\t\treturn %s.create(%s.getPointerIndex(p, %d, %s, i));\n", f->jtype, rootClass, f->offset, f->sizeStr);
					break;
				case ENUM:
					fprintf(out, "\t\treturn %s.values()[p.getInt(%d)%+d];\n", f->jtype, f->offset, f->size);
					break;
				case INT32_ARRAY:
					// FIXME: bounds cfheck
					fprintf(out, "\t\treturn p.getInt(%d+i*4);\n", f->offset);
					break;
				case OFFSET:
					fprintf(out, "\t\treturn %d;\n", f->offset);
					break;
				default:
					break;
				}
				fprintf(out, "\t}\n\n");
			}
			if (f->flags & S) {
				fprintf(out, "\t");
				if (f->flags & P)
					fprintf(out, "public ");
				fprintf(out, "void set%s(%s val) {\n", f->name, f->jtype);
				switch (f->ctype) {
				case INT8:
					fprintf(out, "\t\tp.putByte(%d, val);\n", f->offset);
					break;
				case INT16:
					fprintf(out, "\t\tp.putShort(%d, val);\n", f->offset);
					break;
				case INT32:
					fprintf(out, "\t\tp.putInt(%d, val);\n", f->offset);
					break;
				case INT64:
					fprintf(out, "\t\tp.putLong(%d, val);\n", f->offset);
					break;
				default:
					break;
				}
				fprintf(out, "\t}\n\n");
			}
		}
		

		fprintf(out, "}\n");
		fclose(out);
	}

	// now dump abstract base class
	for (i=0;i<ntypes;i++) {
		struct type *t = &types[i];

		sprintf(fname, "%sAbstract.java", t->name);
		FILE *out = fopen(fname, "w");

		fprintf(out, "// Auto-generated, editing would be pointless\n");
		fprintf(out, "package au.notzed.jjmpeg;\n"
			"import java.nio.ByteBuffer;\n"
			"import java.nio.ByteOrder;\n"
			);
		fprintf(out, "abstract class %sAbstract {\n", t->name);
		fprintf(out, "\tfinal ByteBuffer p;\n"
			"\t%sAbstract(ByteBuffer p) {\n"
			"\t\tthis.p = p;\n"
			"\t\tp.order(ByteOrder.nativeOrder());\n"
			"\t}\n"
			, t->name
			);

		int nfields = t->nfields;
		int j;

		for (j=0;j<nfields;j++) {
			struct field *f = &t->fields[j];

			if (f->flags & G) {
				fprintf(out, "\tabstract ");
				if (f->flags & P)
					fprintf(out, "public ");
				fprintf(out, "%s ", f->jtype);

				if (f->ctype == PTR_INDEX) {
					fprintf(out, "get%sAt(int i);\n", f->name);
				} else if (f->ctype == INT32_ARRAY) {
					fprintf(out, "get%sAt(int i);\n", f->name);
				} else if (f->ctype == OFFSET) {
					fprintf(out, "get%s();\n", f->name);
				} else {
					fprintf(out, "get%s();\n", f->name);
				}
			}
			if (f->flags & S) {
				fprintf(out, "\tabstract ");
				if (f->flags & P)
					fprintf(out, "public ");
				fprintf(out, "void set%s(%s val);\n", f->name, f->jtype);
			}
		}
		

		fprintf(out, "}\n");
		fclose(out);
	}

	return 0;
}

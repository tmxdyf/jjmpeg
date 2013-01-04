#!/usr/bin/perl
# generate ffmpeg bindings

$target = $ARGV[0];
$conf = $ARGV[1];
$abstract = $ARGV[2];
$jni = $ARGV[3];

# hard-code a few supported systems
%targetinfo = (
    "armeabi" => { "size" => 32, "dodl" => 0 },
    "armeabi-v7a" => { "size" => 32, "dodl" => 0 },
    "gnu-amd64" => { "size" => 64, "dodl" => 1 },
    "gnu-i386" => { "size" => 32, "dodl" => 1 },
    "mswin-amd64" => { "size" => 64, "dodl" => 1 },
    "mswin-i386" => { "size" => 32, "dodl" => 1 },
    );

%ti = %{$targetinfo{$target}};
if (not %ti) { die "Unknown target $target specified" };

# size of a pointer
$size = $ti{size};
$dlsymprefix = "d";
$dodl = $ti{dodl};

print "Building $jni and $abstract from $conf\n";

# suffix of generated native binding
$npostfix = "NativeAbstract";
# suffix of generated java binding
$jpostfix = "Abstract";
# suffix of native java implementation
$jimpl = "Native";

if ($size == 64) {
    $resolveObject ="\t:class: *cptr = (:class: *)(*env)->GetLongField(env, jo, :class:_p);\n";
    $resolveObjectField = "(:class: *)(*env)->GetLongField(env, val, :class:_p)";
    $createObject = "(*env)->NewObject(env, :class:_class, :class:_init_p, (jlong):res:)";
    $jptrsig = "J";
    $jnative = "Native64";
} else {
    $resolveObject ="\t:class: *cptr = (:class: *)(*env)->GetIntField(env, jo, :class:_p);\n";
    $resolveObjectField = "(:class: *)(*env)->GetIntField(env, val, :class:_p)";
    $createObject = "(*env)->NewObject(env, :class:_class, :class:_init_p, (jint):res:)";
    $jptrsig = "I";
    $jnative = "Native32";
}

# read api descriptor
open IN,"<$conf";

# c to jni type
%cntype = (
    "int64_t" => "jlong",
# check this
    "long" => "jint",
    "int32_t" => "jint",
    "uint32_t" => "jint",
    "int" => "jint",
    "int16_t" => "jshort",
    "int8_t" => "jbyte",
    "char" => "jbyte",
    "void" => "void",
    "double" => "double",
    "float" => "float",

    "const char *" => "jstring"
    );

%cjtype = (
    "int64_t" => "long",
# check this
    "long" => "int",
    "int32_t" => "int",
    "uint32_t" => "int",
    "int" => "int",
    "int16_t" => "short",
    "int8_t" => "byte",
    "char" => "byte",
    "void" => "void",
    "double" => "double",
    "float" => "float",

    "int *" => "IntBuffer",
    "int16_t *" => "ShortBuffer",
    "uint8_t *" => "ByteBuffer",
    "const char *" => "String",
    "const int16_t *" => "ShortBuffer",
    "const double *" => "DoubleBuffer"
    );

# j type to holder type
# hmm, do i want the specific type here?
%jhtype = (
    "ByteBuffer", "ObjectHolder",
    "IntBuffer", "ObjectHolder",
    "ShortBuffer", "ObjectHolder",
    "StringBuffer", "ObjectHolder",
    );

# function to get the value of a holder type, based on source type (jntype)
# :o is replaced with the object name
# :v is replaced with the value name
%holderGet = (
    "ObjectHolder" => "(:o != NULL ? ADDR((*env)->GetObjectField(env, :o, ObjectHolder_value)) : NULL)"
    );

%holderSet = (
    "ObjectHolder" => "if (:o != NULL) { (*env)->SetObjectField(env, :o, ObjectHolder_value, :v); }"
    );

sub trim($) {
    my $val = shift;
    $val =~ s/^\s+//;
    $val =~ s/\s+$//; 
    return $val;
}

sub doat($$) {
    my $ind = shift;
    my $what = shift;
    if ($ind ne "") {
	print "(".$what.")";
    } else {
	print "()";
    }
}

sub doatjava($$) {
    my $ind = shift;
    my $what = shift;
    if ($ind ne "") {
	print "(".$what.")";
    } else {
	print "()";
    }
}

sub doatcall($$) {
    my $ind = shift;
    my $what = shift;
    if ($ind ne "") {
	print "(".$what.")";
    } else {
	print "()";
    }
}

sub gen_jname($) {
    my $name = shift;
    $name =~ s/^(.)/uc($1)/e;
    $name =~ s/_(.)/uc($1)/ge;
    return $name;
}

sub gen_nname($) {
    my $name = shift;
    $name =~ s/_(.)/_1$1/g;
    return $name;
}

$nativeprefix = "Java_au_notzed_jjmpeg";

# scan in descriptor file
%classes = ();

while (<IN>) {
    next if (m/^#/);
    if (m/class (.*)/) {
	my %classinfo = ();

	$class = $1;
	if ($class =~ m/([^ ]+) (.*)/) {
	    $class = $1;
	    $classinfo{requires} = $2;
	}

	$classinfo{name} = $class;

	my @fields = ();

	# read fields
	while (<IN>) {
	    next if (m/^#/);
	    #if (m/^requires (.*)/) {
	#	$classinfo{requires} = $1;
	#	next;
	 #   }
	    last if (m/^$/) || (m/^methods$/);

	    my %fieldinfo = ();

	    ($type, $name, $opt, $jname, $offset) = split(/,/);
	    if ($jname eq "") {
		$jname = gen_jname($name);
	    } else {
		chomp $jname;
	    }
	    chomp $offset;
	    $ntype = $type;
	    $prefix = "";
	    $scope = "";
	    $nscope = "private ";
	    if ($opt =~ m/p/) {
		$scope = "public ";
	    }
	    if ($opt =~ m/o/) {
		#$jtype = "ByteBuffer";
		$jtype = $type;
		#$prefix = "_";
		$ntype = "jobject";
	    } elsif ($opt =~ m/e/) {
		$jtype = "int";
		#$prefix = "_";
		$ntype = "jint";
	    } else {
		$nscope = $scope;
		$ntype = $cntype{$type};
		$jtype = $cjtype{$type};
	    }
	    if ($opt =~ m/i/) {
		$suffix = "At";
	    } else {
		$suffix = "";
	    }

	    $fieldinfo{name} = $name;
	    $fieldinfo{jname} = $jname;
	    $fieldinfo{prefix} = $prefix;
	    $fieldinfo{opt} = $opt;
	    $fieldinfo{type} = $type;
	    $fieldinfo{jtype} = $jtype;
	    $fieldinfo{ntype} = $ntype;
	    $fieldinfo{scope} = $scope;
	    $fieldinfo{nscope} = $nscope;
	    $fieldinfo{suffix} = $suffix;
	    $fieldinfo{offset} = $offset;

	    push @fields, \%fieldinfo;
	}
	$classinfo{fields} = \@fields;

	# read methods, if any
	if (m/^methods$/) {
	    my @methods = ();
	    $funcprefix = "";
	    $library = "avformat";
	    while (<IN>) {
		next if (m/^#/);
		last if (m/^$/);

		if (m/^prefix (\w*) (\w*)/) {
		    $funcprefix = $1;
		    $library = $2;
		    next;
		}

		($type, $name, $args) = m/^([\w ]*[ \*])(\w*)\((.*)\)/;
		$pname = $name;
		$pname =~ s/$funcprefix//;
		$jname = gen_jname($pname);
		$nname = gen_nname($pname);

		my %methodinfo = ();
		my $static = 0;
		my $wraptype = 0;
		my $scope = "public";

		$type = trim($type);

		if ($type =~ m/optional (.*)/) {
		    $methodinfo{optional} = 1;
		    $type = $1;
		}

		if ($type =~ m/internal (.*)/) {
		    $scope = "internal";
		    $type = $1;
		} elsif ($type =~ m/protected (.*)/) {
		    $scope = "protected";
		    $type = $1;
		} elsif ($type =~ m/native (.*)/) {
		    $scope = "native";
		    $type = $1;
		}

		if ($type =~ m/static (.*)/) {
		    $static = 1;
		    $type = $1;
		}

		if ($type =~ m/(.*) \*$/) {
		    $ctype = $1;
		    $ntype = "jobject";
		    if ($1 eq "void") {
			$jtype = "ByteBuffer";
		    } else {
			$jtype = $1;
			$wraptype = 1;
		    }
		} else {
		    $ctype = $type;
		    $ntype = $cntype{$type};
		    $jtype = $cjtype{$type};
		}

		$rawargs = $args;
		if ($rawargs =~ m/inout/) {
		    # need to fix up our pseudo-prototype to the real one
		    $rawargs =~ s/inout ([^,\)]* \*?)(\w+)/\1*\2/g;
		    $rawargs =~ s/(\w*)\[\]/*\1/g;
		}

		$methodinfo{wraptype} = $wraptype;
		$methodinfo{scope} = $scope;
		$methodinfo{static} = $static;
		$methodinfo{rawargs} = $rawargs;
		$methodinfo{type} = $type;
		$methodinfo{ntype} = $ntype;
		$methodinfo{name} = $name;
		$methodinfo{jtype} = $jtype;
		$methodinfo{ctype} = $ctype;
		$methodinfo{jname} = $jname;
		$methodinfo{nname} = $nname;
		$methodinfo{pname} = $pname;
		$methodinfo{library} = $library;

		my @arginfo = ();

		my $simple = 1;
		my $dofunc = 1;

		@args = split(/,/,$args);
		# first arg is always object pointer
		if (!$static) {
		    my $first = shift @args;
		}
		foreach $a (@args) {
		    ($type, $name, $array) = $a =~ m/^(.*[ \*])(\w+)([\[\]]*)$/;

		    $type = trim($type);
		    $name = trim($name);

		    my %argdata = {};
		    my $deref = 0;
		    my $deenum = 0;

		    if ($type =~ m/inout (.*)/) {
			# TODO: force jtype to be object
			$argdata{direction} = "inout";
			$type = $1;
		    }

		    $argdata{array} = $array eq "[]";
		    $argdata{type} = $type;
		    $argdata{name} = $name;

		    if ($type =~ m/(.*) \*(\*?)$/) {
			$argdata{ctype} = $1;
			$ntype = $cntype{$type};
			if ($ntype eq "") {
			    $ntype = "jobject";
			}
			$argdata{ntype} = $ntype;
			$jtype = $cjtype{$type};
			if ($jtype eq "") {
			    $jtype = $1;
			    #$argdata{jntype} = "ByteBuffer";
			    $argdata{jntype} = $jtype."Native";
			    $deref = 1;
			} else {
			    $argdata{jntype} = $jtype;
			}
			# over-ride jntype for (in)out parameters
			if ($argdata{direction} =~ m/out/) {
			    my $ht = $jhtype{$jtype};
			    if ($ht eq "") {
				$ht = "ObjectHolder";
			    }
			    $argdata{jntype} = $ht;
			    $deref = 0;
			    $jtype = $ht;
			}
			$argdata{jtype} = $jtype;
			$argdata{nname} = "j$name";
			$simple = 0;
			if ($cjtype{$1} ne "") {
			    $dofunc = 0;
			}
		    } elsif ($type =~ m/^enum (.*)$/) {
			$argdata{ntype} = "jint";
			$argdata{jtype} = $1;
			$argdata{jntype} = "int";
			$argdata{nname} = "$name";
			$deenum = 1;
		    } else {
			$argdata{ntype} = $cntype{$type};
			$argdata{jtype} = $cjtype{$type};
			$argdata{jntype} = $cjtype{$type};
			$argdata{nname} = "$name";
		    }

		    $argdata{deref} = $deref;
		    $argdata{deenum} = $deenum;

		    push @arginfo, \%argdata;
		}

		$methodinfo{args} = \@arginfo;
		$methodinfo{simple} = $simple;
		$methodinfo{dofunc} = $dofunc;

		push @methods, \%methodinfo;
	    }
	    $classinfo{methods} = \@methods;
	}

	push @classes, \%classinfo;
    }
}


# create jni code
open STDOUT, ">$jni";

print <<END;
/*
 * Copyright (c) 2011, 2012 Michael Zucchi
 *
 * This file is part of jjmpeg, a java binding to ffmpeg's libraries.
 *
 * jjmpeg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jjmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jjmpeg.  If not, see <http://www.gnu.org/licenses/>.
 */
END
print "// Auto-generated from native.conf\n";

# generate dynamic linkage
if ($dodl) {
    # output function pointers
    foreach $classinfo (@classes) {
	%ci = %{$classinfo};

	my $class = $ci{name};
	my @methods= @{$ci{methods}};

	if ($ci{requires} ne "") {
	    print "#ifdef $ci{requires}\n";
	}
	foreach $methodinfo (@methods) {
	    my %mi = %{$methodinfo};

	    print "static $mi{type} (*${dlsymprefix}$mi{name})($mi{rawargs});\n";
	}
	if ($ci{requires} ne "") {
	    print "#endif\n";
	}
    }
}

# generate method tags
foreach $classinfo (@classes) {
    %ci = %{$classinfo};

    my $class = $ci{name};

    print "static jclass ${class}_class;\n";
    print "static jclass ${class}_native;\n";
    print "static jfieldID ${class}_p;\n";
    print "static jmethodID ${class}_init_p;\n";
}
print "JNIEXPORT jint JNICALL Java_au_notzed_jjmpeg_AVNative_initNative\n";
print "(JNIEnv *env, jclass jc) {\n";
print "\tint res = init_local(env);\n";
print "\tif (res < 0) return res;\n";
print "\n";
print "\tjclass lc;\n";
foreach $classinfo (@classes) {
    %ci = %{$classinfo};

    my $class = $ci{name};

    print "\tlc = (*env)->FindClass(env, \"au/notzed/jjmpeg/$class\");\n";
    print "\tif (!lc) return -1;\n";
    print "\t${class}_class = (*env)->NewGlobalRef(env, lc);\n";
    print "\t(*env)->DeleteLocalRef(env, lc);\n";
    print "\t${class}_init_p = (*env)->GetMethodID(env, ${class}_class, \"<init>\", \"($jptrsig)V\");\n";
    print "\tif (!${class}_init_p) { printf(\"No init: ${class} \\\"($jptrsig)V\\\" \\n\"); fflush(stdout); return -1; }\n";

    print "\tlc = (*env)->FindClass(env, \"au/notzed/jjmpeg/${class}${jnative}\");\n";
    print "\tif (!lc) return -1;\n";
    print "\t${class}_native = (*env)->NewGlobalRef(env, lc);\n";
    print "\t(*env)->DeleteLocalRef(env, lc);\n";
    print "\t${class}_p = (*env)->GetFieldID(env, ${class}_native, \"p\", \"$jptrsig\");\n";
    print "\tif (!${class}_p) { printf(\"No field p: ${class} \\\"($jptrsig)V\\\" \\n\"); fflush(stdout); return -1; }\n";
    #print "\tprintf(\"${class}${jnative}.p = %p\\n\", ${class}_p);\n";
}

if ($dodl) {
    foreach $classinfo (@classes) {
	%ci = %{$classinfo};

	my $class = $ci{name};
	my @methods= @{$ci{methods}};

	if ($ci{requires} ne "") {
	    print "#ifdef $ci{requires}\n";
	}

	foreach $methodinfo (@methods) {
	    my %mi = %{$methodinfo};

	    # man dlopen says to do this hacked up shit because of c99, but gcc whines rightly about it
	    if ($mi{optional}) {
		printf "\tMAPDLIF($mi{name}, $mi{library}_lib);\n";
	    } else {
		printf "\tMAPDL($mi{name}, $mi{library}_lib);\n";
	    }
	}

	if ($ci{requires} ne "") {
	    print "#endif\n";
	}
    }
}

print "\tres = init_platform(env);\n";
print "\tif (res < 0) return res;\n";
print "\n";
print "\treturn sizeof(void *)*8;\n";
print "}\n";

foreach $classinfo (@classes) {
    my %ci = %{$classinfo};

    my $class = $ci{name};

    print "\n\n/* Class: $class */\n\n";

    # field accessors
    my @fields = @{$ci{fields}};

    if ($ci{requires} ne "") {
	print "#ifdef $ci{requires}\n";
    }
    
    foreach $fieldinfo (@fields) {
	my %fi = %{$fieldinfo};

	# getter
	my $opt = $fi{opt};
	my $ind = $opt =~ m/i/;

	if ($opt =~ m/g/) {
	    print "JNIEXPORT $fi{ntype} JNICALL ${nativeprefix}_${class}${npostfix}_";
	    #if ($opt =~ m/[eo]/) {
	#	print "_1";
	    #}
	    if ($opt =~ m/i/) {
		#print "1";
		$at = "At";
	    } else {
		$at = "";
	    }
	    print "get$fi{jname}$at(";
	    print "JNIEnv *env, jobject jo";
	    if ($ind) {
		print ", jint index";
	    }
	    print ") {\n";
	    my $res = $resolveObject;
	    $res =~ s/:class:/$class/g;
	    print $res;
	    #print "\t$class *cptr = ADDR(jptr);\n";
	    if ($fi{ntype} eq "jobject" or $fi{ntype} eq "jstring") {
		print "\tvoid *cdata = (void *)";
		if ($opt =~ m/r/) {
		    print "&";
		}
		print "cptr->$fi{name}";
		if ($ind) {
		    print "[index]";
		}
		print ";\n";
		print "\tif (cdata == NULL) return NULL;\n";
		print "\treturn ";
		if ($fi{ntype} eq "jobject") {
		    my $c = $createObject;

		    $c =~ s/:class:/$fi{type}/g;
		    $c =~ s/:res:/cdata/g;
		    print "$c;\n";
		    #print "WRAP(";
		    #print "cdata, sizeof($fi{type}));\n";
		} elsif ($fi{ntype} eq "jstring") {
		    print "WRAPSTR((char *)cdata);\n";
		}
	    } else {
		print "\treturn ";
		print "cptr->$fi{name}";
		if ($ind) {
		    print "[index]";
		}
		print ";\n";
	    }
	    print "}\n\n";
	}
	if ($opt =~ m/s/) {
	    print "JNIEXPORT void JNICALL ${nativeprefix}_${class}${npostfix}_";
	    #if ($opt =~ m/[eo]/) {
	#	print "_1";
	    #}
	    if ($opt =~ m/i/) {
		#print "1";
		$at = "At";
	    } else {
		$at = "";
	    }
	    print "set$fi{jname}$at(";
	    print "JNIEnv *env, jobject jo";
	    if ($ind) {
		print ", jint index";
	    }
	    print ", $fi{ntype} val";
	    print ") {\n";
	    #print "\t$class *cptr = ($class *)(*env)->GetIntField(env, jo, ${class}_p);\n";
	    my $res = $resolveObject;
	    $res =~ s/:class:/$class/g;
	    print $res;
	    #print "\tjobject jptr = (*env)->GetObjectField(env, jo, field_p);\n";
	    #print "\t$class *cptr = ADDR(jptr);\n";

	    if ($fi{ntype} eq "jobject") {
		my $r = $resolveObjectField;

		$r =~ s/:class:/$fi{type}/g;

		print "\t$fi{type} *cval = $r;\n";
	    }

	    print "\tcptr->$fi{name}";
	    if ($ind) {
		print "[index]";
	    }
	    print " = ";
	    if ($fi{ntype} eq "jobject") {
		print "cval";
	    } else {
		print "val";
	    }
	    #if ($opt =~ m/o/) {
	#	print "ADDR(";
	#    }
	#    print "val";
	#    if ($opt =~ m/o/) {
	#	print(")");
	#    }
	    print ";\n}\n\n";
	}
    }

    # methods
    my @methods= @{$ci{methods}};
    foreach $methodinfo (@methods) {
	my %mi = %{$methodinfo};
	my @arginfo = @{$mi{args}};

	next if ($mi{scope} eq "internal");

	print "JNIEXPORT $mi{ntype} JNICALL ${nativeprefix}_${class}${npostfix}_$mi{nname}\n";
	if ($mi{static}) {
	    print "(JNIEnv *env, jclass jc";
	    foreach $argdata (@arginfo) {
		%ai = %{$argdata};
		print ", $ai{ntype} $ai{nname}";
	    }
	    print ") {\n";
	} else {
	    print "(JNIEnv *env, jobject jo";
		
	    foreach $argdata (@arginfo) {
		%ai = %{$argdata};
		print ", $ai{ntype} $ai{nname}";
	    }
	    print ") {\n";
	    my $res = $resolveObject;
	    $res =~ s/:class:/$class/g;
	    print $res;
	    #print "\t$class *cptr = ($class *)(*env)->GetIntField(env, jo, ${class}_p);\n";
	    #print "\t$class *cptr = ADDR(jptr);\n";
	}

	# check 'optional' functions at the C level to avoid JVM crashes.
	if ($dodl && $mi{optional}) {
	    print " if (!${dlsymprefix}$mi{name}) {\n";
	    print "  throwException(env, \"java/lang/NoSuchMethodError\", \"$mi{name}\");\n";
	    if ($mi{type} ne "void") {
		print "  return 0;\n";
	    } else {
		print "  return;\n";
	    }
	    print " }\n";
	}

	# wrap/converty any jni args to c args
	foreach $argdata (@arginfo) {
	    %ai = %{$argdata};
	    if ($ai{direction} eq "inout") {
		my $hg = $holderGet{$ai{jntype}};

		$hg =~ s/:o/$ai{nname}/g;

		print "\t$ai{type} $ai{name} = $hg;\n";
	    } elsif ($ai{direction} eq "out") {
		print "\t$ai{type} $ai{name};\n";
	    } else {
		if ($ai{ntype} eq "jobject") {
		    if ($ai{jntype} =~ m/Buffer$/) {
			print "\t$ai{type} $ai{name} = ADDR($ai{nname});\n";
		    } else {
			print "\t$ai{type} $ai{name} = PTR($ai{nname}, $ai{ctype});\n";
		    }
		}
		if ($ai{ntype} eq "jstring") {
		    print "\t$ai{type} $ai{name} = STR($ai{nname});\n";
		}
	    }
	}
	print "\n";
	
	# call function
	if ($mi{ntype} eq "jobject") {
	    print "\tvoid *cres = ";
	} elsif ($mi{type} ne "void") {
	    print "\t$mi{ntype} res = ";
	} else {
	    print "\t";
	}
	if ($dodl) {
	    print "(*${dlsymprefix}";
	}
	print "$mi{name}";
	if ($dodl) {
	    print ")";
	}
	print "(";
	$count = 0;
	if (!$mi{static}) {
	    print "cptr";
	    $count = 1;
	}
	foreach $argdata (@arginfo) {
	    %ai = %{$argdata};
	    if ($count > 0) {
		print ", ";
	    }
	    if ($ai{direction} =~ m/out/) {
		print "&";
	    }
	    print "$ai{name}";
	    $count++;
	}
	print ")";
	print ";\n";
	if ($mi{ntype} eq "jobject") {
	    my $c = $createObject;
	    $c =~ s/:res:/cres/g;
	    $c =~ s/:class:/$mi{ctype}/g;
	    print "\t$mi{ntype} res = cres ? $c : NULL;\n";
	}
	print "\n";
	# post-process arguments
	foreach $argdata (@arginfo) {
	    %ai = %{$argdata};
	    # handle out paramters
	    if ($ai{direction} =~ m/out/) {
		my $hs = $holderSet{$ai{jntype}};

		$hs =~ s/:o/$ai{nname}/g;

		if ($ai{ntype} eq "jobject") {
		    my $c = $createObject;

		    $c =~ s/:class:/$ai{ctype}/g;
		    $c =~ s/:res:/$ai{name}/g;
		    #$hs =~ s/:v/WRAP($ai{name}, sizeof(*$ai{name}))/;
		    $hs =~ s/:v/$c/;
		} else {
		    $hs =~ s/:v/$ai{name}/g;
		}

		print "\t$hs\n";
	    }

	    # free strings
	    if ($ai{ntype} eq "jstring") {
		print "\tRSTR($ai{nname}, $ai{name});\n";
	    }
	}

	if ($mi{type} ne "void") {
	    print "\treturn res;\n";
	}
	print "}\n\n";
    }

    if ($ci{requires} ne "") {
	print "#endif\n";
    }
}

close STDOUT;
open STDOUT, ">$abstract";

print "/* I am automatically generated.  Editing me would be pointless,\n   but I wont stop you if you so desire. */\n\n";

print <<END;
package au.notzed.jjmpeg;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.DoubleBuffer;

END

# now create java code
foreach $classinfo (@classes) {
    my %ci = %{$classinfo};

    my $class = $ci{name};

    # First the native wrapper
    print "abstract class ${class}${npostfix} extends AVNative {\n";
    print "\tprotected ${class}${npostfix}(AVObject o) {\n";
    print "\t\tsuper(o);\n";
    print "\t}\n";

    print "\t// Fields\n";
    # field accessors
    my @fields = @{$ci{fields}};
    foreach $fieldinfo (@fields) {
	my %fi = %{$fieldinfo};

	# getter
	my $opt = $fi{opt};
	my $ind = $opt =~ m/i/;

	if ($opt =~ m/g/) {
	    print "\tnative $fi{jtype} $fi{prefix}get$fi{jname}$fi{suffix}";
	    doat($ind, "int index");
	    print ";\n";
	}
	if ($opt =~ m/s/) {
	    print "\tnative void $fi{prefix}set$fi{jname}$fi{suffix}(";
	    #print "ByteBuffer p, ";
	    if ($ind) {
		print "int index, ";
	    }
	    if ($opt =~ m/o/) {
		print "$fi{jtype}Native val";
	    } else {
		print "$fi{jtype} val";
	    }
	    print ");\n";
	}
    }
    
    # methods
    print "\t// Native Methods\n";
    my @methods= @{$ci{methods}};
    foreach $methodinfo (@methods) {
	my %mi = %{$methodinfo};
	my $name = $mi{pname};
	my $scope = "";

	next if ($mi{scope} eq "internal");

	if ($mi{static}) {
	    $scope = "static ".$scope;
	}

	$jtype = $mi{jtype};
	#if ($mi{wraptype}) {
	#    $jtype = "ByteBuffer";
	#}

	print "\t$scope native ${jtype} ${name}(";
	my @arginfo = @{$mi{args}};
	my $count = 0;

	#if (!$mi{static}) {
	#    $count = 1;
	#    print "ByteBuffer p";
	#}

	foreach $argdata (@arginfo) {
	    %ai = %{$argdata};
	    print ", " if $count > 0;
	    print "$ai{jntype} $ai{name}";
	    $count += 1;
	}
	print ");\n";
    }

    print "}\n\n";

    # Now the java accessor to the native object
    print "abstract class ${class}$jpostfix extends AVObject {\n";

    print "\t$class$jimpl n;\n";

    print "\tfinal protected void setNative($class$jimpl n) {\n";
    print "\t\tthis.n = n;\n";
    print "\t}\n";

    print "\tpublic void dispose() {\n";
    print "\t\tn.dispose();\n";
    print "\t}\n";

    $aclass = "$class$npostfix.";

    print "\t// Fields\n";
    # field accessors
    my @fields = @{$ci{fields}};
    foreach $fieldinfo (@fields) {
	my %fi = %{$fieldinfo};

	# getter
	my $opt = $fi{opt};
	my $ind = $opt =~ m/i/;

	if ($opt =~ m/g/) {
	    if ($opt =~ m/o/) {
		print "\t$fi{scope} $fi{type} get$fi{jname}$fi{suffix}";
		doatjava($ind, "int index");
		#print " {\n\t\treturn $fi{type}.create(${aclass}get$fi{jname}$fi{suffix}";
		print " {\n\t\treturn n.get$fi{jname}$fi{suffix}";
		doatcall($ind, "index");
		print ";\n\t}\n";
	    } elsif ($opt =~ m/e/) {
		print "\t${scope} $fi{type} get$fi{jname}() {\n";
		print "\t\treturn $fi{type}.values()[n.get$fi{jname}()+$fi{offset}];\n\t}\n";
	    } else {
		print "\t$fi{scope} $fi{jtype} get$fi{jname}$fi{suffix}";
		doatjava($ind, "int index");
		print " {\n\t\treturn n.get$fi{jname}$fi{suffix}";
		doatcall($ind, "index");
		print ";\n\t}\n";
	    }
	}
	if ($opt =~ m/s/) {
	    if ($opt =~ m/o/) {
		print "\t${scope} void set$fi{jname}($fi{type} val) {\n";
		print "\t\tn.set$fi{jname}(val.n);\n\t}\n";
	    } elsif ($opt =~ m/e/) {
		print "\t${scope} void set$fi{jname}($fi{type} val) {\n";
		print "\t\tn.set$fi{jname}(val.toC());\n\t}\n";
	    } else {
		print "\t${scope} void set$fi{jname}($fi{jtype} val) {\n";
		print "\t\tn.set$fi{jname}(val);\n\t}\n";
	    }
	}
    }

    print "\t// Public Methods\n";
    foreach $methodinfo (@methods) {
	my %mi = %{$methodinfo};
	my @arginfo = @{$mi{args}};

	# add the public wrapper - if it's simple and we can
	my $name = $mi{jname};
	my $abstract = "";
	my $scope = $mi{scope};

	next if ($scope eq "native");
	next if ($scope eq "internal");

	if(!$mi{dofunc}) {
	    $scope = "";
	    #next;
	    #$abstract = "abstract ";
	}

	if ($mi{static}) {
	    #$scope = "";
	    $abstract = "static ".$abstract;
	}

	$name =~ s/^(.)/lc($1)/e;
	print "\t${abstract}${scope} $mi{jtype} ${name}(";
	$count = 0;
	foreach $argdata (@arginfo) {
	    %ai = %{$argdata};
	    print ", " if $count > 0;
	    print "$ai{jtype} $ai{name}";
	    $count += 1;
	}
	print ")";
	#if ($mi{dofunc}) {
	if (1) {
	    print " {\n\t\t";
	    if ($mi{jtype} ne "void") {
		print "return ";
	    }
	#    if ($mi{wraptype}) {
	#	print "$mi{jtype}.create(";
	#    }
	    if ($mi{static}) {
		print "${aclass}";
	    } else {
		print "n.";
	    }
	    print "$mi{pname}(";
	    $count = 0;
	#    if (!$mi{static}) {
	#	$count = 1;
	#	print "n.p";
	#    }
	    foreach $argdata (@arginfo) {
		%ai = %{$argdata};
		print ", " if $count > 0;
		if ($ai{deref}) {
		    print "$ai{name} != null ? ";
		}
		print "$ai{name}";
		if ($ai{deenum}) {
		    print ".toC()";
		}
		if ($ai{deref}) {
		    print ".n : null";
		}
		$count += 1;
	    }
	    print ")";
	#    if ($mi{wraptype}) {
	#	print ")";
	#    }
	    print ";\n\t}\n";	    
	} else {
	    print ";\n";
	}
    }
    print "}\n";
}
    close STDOUT;

exit

@
@ Copyright (c) 2013 Michael Zucchi
@
@ This file is part of jjmpeg.
@
@ jjmpeg is free software: you can redistribute it and/or modify
@ it under the terms of the GNU General Public License as published by
@ the Free Software Foundation, either version 3 of the License, or
@ (at your option) any later version.
@
@ jjmpeg is distributed in the hope that it will be useful,
@ but WITHOUT ANY WARRANTY; without even the implied warranty of
@ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
@ GNU General Public License for more details.
@
@ You should have received a copy of the GNU General Public License
@ along with jjmpeg.  If not, see <http://www.gnu.org/licenses/>.

	.arm
	.arch	armv7-a
	.fpu	neon

@
@ This is a fast-but-approximate YUV to RGB conversion
@ routine.
@
@ It utilises 8x8 bit signed multiplication, and so only
@ 7 bits are available for the scaling factors.
@
	
	@ straight yuv to rgb conversion
	@ yuv are all at same resolution

	@ r0 srcy
	@ r1 srcu
	@ r2 srcv
	@ r3 dst
	@ r4 width, must be >= 16, multiple of 16

	@ TODO: improve scheduling, as with the rgb565 version below.
	
	.global yuv_rgb_neon
yuv_rgb_neon:
	stmdb sp!, {r4, lr}

	@ approximate scale in 2:14 format
	vmov.u16	q12,#16383
	vmov.u8		q13,#0
	
	vmov.u8		q3,#128

	vmov.u8	d28,#90			@ 1.402 * 64
	vmov.u8	d29,#113		@ 1.772 * 64
	vmov.u8	d30,#22			@ 0.34414 * 64
	vmov.u8	d31,#46			@ 0.71414 * 64

	ldr	r4,[sp,#2*4]
1:	
	vld1.u8	{ d0, d1 }, [r0]!	@ y is 0-255
	vld1.u8	{ d2, d3 }, [r1]!	@ u is -128-127
	vld1.u8	{ d4, d5 }, [r2]!	@ v is -128-127

	vshll.u8	q10,d0,#6	@ y * 64
	vshll.u8	q11,d1,#6

	vsub.s8		q1,q3		@ u -= 128
	vsub.s8		q2,q3		@ v -= 128
	
	vmull.s8	q12,d29,d2	@ u * 1.772
	vmull.s8	q13,d29,d3

	vmull.s8	q8,d28,d4	@ v * 1.402
	vmull.s8	q9,d28,d5

	vadd.s16	q12,q10		@ y + 1.722 * u
	vadd.s16	q13,q11
	vadd.s16	q8,q10		@ y + 1.402 * v
	vadd.s16	q9,q11

	vmlsl.s8	q10,d30,d2	@ y -= 0.34414 * u
	vmlsl.s8	q11,d30,d3
	vmlsl.s8	q10,d31,d4	@ y -= 0.71414 * v
	vmlsl.s8	q11,d31,d5

	@ currently shorts, 2:14 format
	@ clamp before scaling

	subs	r4,#16

	.if 0
	vmov.u8		q0,#0
	vmov.u16	q1,#16383

	vmax.s16	q8,q0
	vmax.s16	q9,q0
	vmax.s16	q10,q0
	vmax.s16	q11,q0
	vmax.s16	q12,q0
	vmax.s16	q13,q0
	
	vmin.s16	q8,q1
	vmin.s16	q9,q1
	vmin.s16	q10,q1
	vmin.s16	q11,q1
	vmin.s16	q12,q1
	vmin.s16	q13,q1

	@ rescale & output
	vshrn.i16	d16,q8,#6
	vshrn.i16	d17,q9,#6
	vshrn.i16	d18,q10,#6
	vshrn.i16	d19,q11,#6
	vshrn.i16	d20,q12,#6
	vshrn.i16	d21,q13,#6
	.endif

	.if 1
	@ scale & clamp?
	vqshrun.s16	d16,q8,#6
	vqshrun.s16	d17,q9,#6
	vqshrun.s16	d18,q10,#6
	vqshrun.s16	d19,q11,#6
	vqshrun.s16	d20,q12,#6
	vqshrun.s16	d21,q13,#6
	.endif
	
	vst3.u8		{ d16,d18,d20 },[r3]!
	vst3.u8		{ d17,d19,d21 },[r3]!
	

	bhi	1b

	ldmfd sp!, {r4, pc}

	@ yuv 420 to rgb565 conversion

	@ r0 srcy
	@ r1 srcu
	@ r2 srcv
	@ r3 dst
	@ r4 width, must be >= 16, multiple of 16

	@ TODO: optimise scheduling

	@ 54 cycles inner loop with clamp
	@ 49 inner loop with implicit clamp
	.global yuv420p_rgb565_neon_u
yuv420p_rgb565_neon_u:
	stmdb sp!, {r4, lr}

	@ approximate scale in 2:14 format
	vmov.u16	q12,#16383
	vmov.u8		q13,#0
	
	vmov.u8		q3,#128

	vmov.u8	d28,#90			@ 1.402 * 64
	vmov.u8	d29,#113		@ 1.772 * 64
	vmov.u8	d30,#22			@ 0.34414 * 64
	vmov.u8	d31,#46			@ 0.71414 * 64

	ldr	r4,[sp,#2*4]
1:	
	vld1.u8	{ d0, d1 }, [r0]!	@ y is 0-255
	vld1.u8	{ d2 }, [r1]!	@ u is -128-127
	vld1.u8	{ d4 }, [r2]!	@ v is -128-127

	@ double UV
	vmov	d3,d2
	vmov	d5,d4
	vzip.8	d2,d3
	vzip.8	d4,d5

	vshll.u8	q10,d0,#6	@ y * 64
	vshll.u8	q11,d1,#6

	vsub.s8		q1,q3		@ u -= 128
	vsub.s8		q2,q3		@ v -= 128
	
	vmull.s8	q12,d29,d2	@ u * 1.772
	vmull.s8	q13,d29,d3

	vmull.s8	q8,d28,d4	@ v * 1.402
	vmull.s8	q9,d28,d5

	vadd.s16	q12,q10		@ y + 1.722 * u
	vadd.s16	q13,q11
	vadd.s16	q8,q10		@ y + 1.402 * v
	vadd.s16	q9,q11

	vmlsl.s8	q10,d30,d2	@ y -= 0.34414 * u
	vmlsl.s8	q11,d30,d3
	vmlsl.s8	q10,d31,d4	@ y -= 0.71414 * v
	vmlsl.s8	q11,d31,d5

	@ currently shorts, 2:14 format

	subs	r4,#16

	@ clamp
	.if 0
	vmov.u8		q0,#0
	vmov.u16	q1,#16383

	vmax.s16	q8,q0
	vmax.s16	q9,q0
	vmax.s16	q10,q0
	vmax.s16	q11,q0
	vmax.s16	q12,q0
	vmax.s16	q13,q0
	
	vmin.s16	q8,q1
	vmin.s16	q9,q1
	vmin.s16	q10,q1
	vmin.s16	q11,q1
	vmin.s16	q12,q1
	vmin.s16	q13,q1

	@ to rgb 565
	@ we know the value is within range 16383 ... 0, i.e. upper 2 bits are zero.
	@ not that it helps with the insert instruction
	
	vshl.i16	q8,#2		@ red in upper 8 bits
	vshl.i16	q9,#2
	vshl.i16	q10,#2		@ green in upper 8 bits
	vshl.i16	q11,#2
	vshl.i16	q12,#2		@ blue in upper 8 bits
	vshl.i16	q13,#2
	.endif

	.if 1
	vqshlu.s16	q8,#2		@ red in upper 8 bits
	vqshlu.s16	q9,#2
	vqshlu.s16	q10,#2		@ green in upper 8 bits
	vqshlu.s16	q11,#2
	vqshlu.s16	q12,#2		@ blue in upper 8 bits
	vqshlu.s16	q13,#2
	.endif
	
	vsri.16		q8,q10,#5	@ insert green
	vsri.16		q9,q11,#5
	vsri.16		q8,q12,#11	@ insert blue
	vsri.16		q9,q13,#11

	@ output
	vst1.u16	{ d16,d17,d18,d19 },[r3]!

	bhi	1b

	ldmfd sp!, {r4, pc}
	

	@ optimised version
	@ 35 cycles inner loop
	.global yuv420p_rgb565_neon
yuv420p_rgb565_neon:
	stmdb sp!, {r4, lr}

	@ approximate scale in 2:14 format
	vmov.u16	q12,#16383
	vmov.u8		q13,#0
	
	vmov.u8		q3,#128

	vmov.u8	d28,#90			@ 1.402 * 64
	vmov.u8	d29,#113		@ 1.772 * 64
	vmov.u8	d30,#22			@ 0.34414 * 64
	vmov.u8	d31,#46			@ 0.71414 * 64

	ldr	r4,[sp,#2*4]

	@ init first loop
	vld1.u8	{ d0, d1 }, [r0]!	@ y is 0-255
	vld1.u8	{ d2 }, [r1]!	@ u is -128-127
	vld1.u8	{ d4 }, [r2]!	@ v is -128-127

	vsub.s8 d3,d2,d6		@ u-= 128
	vsub.s8 d2,d2,d6
	vsub.s8	d5,d4,d6		@ v-= 128
	vsub.s8 d4,d4,d6	
	vzip.8	d2,d3			@ double UV
	vzip.8	d4,d5

	vshll.u8	q10,d0,#6	@ y * 64
	vshll.u8	q11,d1,#6
1:	
	subs	r4,#16

	vmull.s8	q12,d29,d2	@ u * 1.772
	vmull.s8	q13,d29,d3

	vmull.s8	q8,d28,d4	@ v * 1.402
	vmull.s8	q9,d28,d5

	bls	2f
	vld1.u8	{ d0, d1 }, [r0]!	@ y is 0-255
2:
	vadd.s16	q12,q10		@ y + 1.722 * u
	vadd.s16	q13,q11
	vadd.s16	q8,q10		@ y + 1.402 * v
	vadd.s16	q9,q11

	vmlsl.s8	q10,d30,d2	@ y -= 0.34414 * u
	vmlsl.s8	q11,d30,d3
	vmlsl.s8	q10,d31,d4	@ y -= 0.71414 * v
	vmlsl.s8	q11,d31,d5

	bls	2f
	vld1.u8	{ d2 }, [r1]!	@ u is -128-127
	vld1.u8	{ d4 }, [r2]!	@ v is -128-127
2:	
	vsub.s8 d3,d2,d6		@ u-= 128
	vsub.s8 d2,d2,d6
	vsub.s8	d5,d4,d6		@ v-= 128
	vsub.s8 d4,d4,d6	
	
	@ clamp/scale
	vqshlu.s16	q8,#2		@ red in upper 8 bits
	vqshlu.s16	q9,#2
	vqshlu.s16	q10,#2		@ green in upper 8 bits
	vqshlu.s16	q11,#2
	vqshlu.s16	q12,#2		@ blue in upper 8 bits
	vqshlu.s16	q13,#2

	vzip.8	d2,d3			@ double UV
	vzip.8	d4,d5

	vsri.16		q8,q10,#5	@ insert green
	vsri.16		q9,q11,#5
	vsri.16		q8,q12,#11	@ insert blue
	vsri.16		q9,q13,#11

	vshll.u8	q10,d0,#6	@ y * 64
	vshll.u8	q11,d1,#6

	@ output
	vst1.u16	{ d16,d17,d18,d19 },[r3]!

	bhi	1b

	ldmfd sp!, {r4, pc}
	


	
	@ expand bytes twice as wide
	
	@ r0 src
	@ r1 dst
	@ r2 size
	
	.global	expand_2x
expand_2x:

1:	
	vld1.u8	{ d0, d1 }, [r0]!

	vshll.u8	q8,d0,#8
	vmovl.u8	q10,d0
	vshll.u8	q9,d1,#8
	vmovl.u8	q11,d1

	vorr		q8,q10
	vorr		q9,q11

	subs	r2,#16
	vst1.u8		{ d16, d17, d18, d19 },[r1]!

	bhi	1b

	mov	pc,lr
	

	
	@ expand uv twice as wide
	
	@ r0 src u
	@ r1 dst u
	@ r2 src v
	@ r3 dst v
	@ r4 size
	
	.global	expand_uv
expand_uv:	
	stmdb sp!, {r4, lr}

	ldr	r4,[sp,#2*4]

1:	
	vld1.u8	{ d0, d1 }, [r0]!
	vld1.u8	{ d2, d3 }, [r2]!

	vshll.u8	q8,d0,#8
	vmovl.u8	q10,d0
	vshll.u8	q9,d1,#8
	vmovl.u8	q11,d1

	vshll.u8	q12,d2,#8
	vmovl.u8	q14,d2
	vshll.u8	q13,d3,#8
	vmovl.u8	q15,d3

	vorr		q8,q10
	vorr		q9,q11
	
	vorr		q12,q14
	vorr		q13,q15

	subs	r4,#16
	
	vst1.u8		{ d16, d17, d18, d19 },[r1]!
	vst1.u8		{ d24, d25, d26, d27 },[r3]!

	bhi	1b

	ldmfd sp!, {r4, pc}
	

	


	@ version 2 - use shorts and vqdmulh to get the results without scaling required

	.global yuv_rgb_neon2
yuv_rgb_neon2:
	stmdb sp!, {r4, lr}

	ldr	r4,[sp,#2*4]

	@ approximate scale in 2:14 format
	vmov.u16	q11,#128

	ldr	r4,=45941
	vdup.u16	q12,r4
	ldr	r4,=58064
	vdup.u16	q13,r4
	ldr	r4,=11276
	vdup.u16	q14,r4
	ldr	r4,=23401
	vdup.u16	q15,r4
	
	@vmov.u16	q12,#45941		@ 1.402 * 65536 / 2
	@vmov.u16	q13,#58064		@ 1.772 * 64
	@vmov.u16	q14,#11276		@ 0.34414 * 64
	@vmov.u16	q15,#23401		@ 0.71414 * 64
1:	
	vld1.u8	{ d2, d3 }, [r0]!	@ y is 0-255
	vld1.u8	{ d6, d7 }, [r1]!	@ u is -128-127
	vld1.u8	{ d10, d11 }, [r2]!	@ v is -128-127

	vmovl.u8	q0,d2		@ to short
	vmovl.u8	q1,d3
	vmovl.u8	q2,d6
	vmovl.u8	q3,d7
	vmovl.u8	q4,d8
	vmovl.u8	q5,d9

	vsub.u8		q2,q11		@ u -= 128
	vsub.u8		q3,q11
	vsub.u8		q4,q11		@ v -= 128
	vsub.u8		q5,q11

	vqdmulh.s16	q6,q13,q2	@ u * 1.772
	vqdmulh.s16	q7,q13,q3

	vqdmulh.s16	q8,q12,q4	@ v * 1.402
	vqdmulh.s16	q9,q12,q5

	vqdmulh.s16	q2,q14
	vqdmulh.s16	q3,q14
	vqdmulh.s16	q4,q15
	vqdmulh.s16	q5,q15
	
	vadd.s16	q6,q0		@ y + 1.722 * u
	vadd.s16	q7,q1
	vadd.s16	q8,q0		@ y + 1.402 * v
	vadd.s16	q9,q1

	vsub.s16	q0,q2		@ y -= 0.34414 * u
	vsub.s16	q1,q3
	vsub.s16	q2,q4		@ y -= 0.71414 * v
	vsub.s16	q3,q5
	
	@ currently shorts, 2:14 format
	@ clamp before scaling

	.if 1
	vmov.u8		q0,#0
	vmov.u16	q1,#255

	vmax.s16	q8,q0
	vmax.s16	q9,q0
	vmax.s16	q10,q0
	vmax.s16	q11,q0
	vmax.s16	q12,q0
	vmax.s16	q13,q0
	
	vmin.s16	q8,q1
	vmin.s16	q9,q1
	vmin.s16	q10,q1
	vmin.s16	q11,q1
	vmin.s16	q12,q1
	vmin.s16	q13,q1
	.endif

	subs	r4,#16

	@ rescale & output
	vshrn.i16	d16,q8,#6
	vshrn.i16	d17,q9,#6
	vshrn.i16	d18,q10,#6
	vshrn.i16	d19,q11,#6
	vshrn.i16	d20,q12,#6
	vshrn.i16	d21,q13,#6
	
	vst3.u8		{ d16,d18,d20 },[r3]!
	vst3.u8		{ d17,d19,d21 },[r3]!
	

	bhi	1b

	ldmfd sp!, {r4, pc}


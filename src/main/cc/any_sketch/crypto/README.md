# Elgamal Public Key Combiner

A valid Elgamal public Key contains a generator (g) and an element (y). In order
to combine the keys, all keys' generators should have the same value. Since we
use the default generator for all keys, only the elements are required to use
the combiner.

## Flag to set

*   curve_id: the Elliptic curve id.
*   elgamal_y_list: the list of ElGamal Y keys to combine.

### Example

```
combine_public_keys  --curve_id=415  --elgamal_y_list=02d1432ca007a6c6d739fce2d21feb56d9a2c35cf968265f9093c4b691e11386b3,039ef370ff4d216225401781d88a03f5a670a5040e6333492cb4e0cd991abbd5a3,02d0f25ab445fc9c29e7e2509adc93308430f432522ffa93c2ae737ceb480b66d7
```

## Expected output

curveId: 415 \
Keys to combine: \
02d1432ca007a6c6d739fce2d21feb56d9a2c35cf968265f9093c4b691e11386b3 \
039ef370ff4d216225401781d88a03f5a670a5040e6333492cb4e0cd991abbd5a3 \
02d0f25ab445fc9c29e7e2509adc93308430f432522ffa93c2ae737ceb480b66d7

Result: \
02505d7b3ac4c3c387c74132ab677a3421e883b90d4c83dc766e400fe67acc1f04

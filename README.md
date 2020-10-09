# Any Sketch

**Table of Contents**
  * [Overview](#overview)
  * [Details](#details)
    * [Developer Guide](#developer-guide)
    * [Developer Environment](#developer-environment)
  * [How to Build](#how-to-build)
  * [How to Deploy](#how-to-deploy)
  * [Documentation](#documentation)
    * [Dependencies](#dependencies)
  * [Contributing](#contributing)

## Overview

This repo contains the implementation of the AnySketch data structure in C++
and Java. AnySketch is a generic data structure that can be configured to act
as
  * [HyperLogLog](https://datasketches.apache.org/docs/HLL/HLL.html)
  * [LiquidLegions](https://research.google/pubs/pub49177/)
  * [CascadingLegions](https://research.google/pubs/pub49177/)
  * Other sketches that can be defined with an index function and an aggregator.

## Details

AnySketch stores key-value pairs in a sparse format. AnySketch config is
defined by:
1. A List of indexes, each with a Distribution,
1. A List of value columns, each with a distribution,
1. Aggregation functions for each value.

When a key is added to a sketch it is mapped to a vector of indexes and vector of values.
For each vector of indexes the vectors of values are aggregated with the aggregation functions
defined by the config.

### Examples

Let's say we want to create a counting Bloom Filter.
Here is what are sketch config would look like

```
indexes {
  name: "Index"
  distribution {
    uniform {
      num_values: 10000000  # 10M
    }
  }
}
values {
  name: "Frequency"
  aggregator: SUM
  distribution {
    oracle {
      key: "frequency"
    }
  }
}
```

Create the sketch and insert elements to it

```
AnySketch anySketch = SketchProtos.toAnySketch(sketchConfig);
anySketch.insert(123456, ImmutableMap.of("frequency", 1L));
anySketch.insert(999999, ImmutableMap.of("frequency", 1L));
```

In this example, we did not store any values like demographics in the registers,
thus the empty HashMap.
AnySketch hashed the keys (123456 and 999999), distributed the hash uniformly to 10M values
and put the value 1 in those two registers. In the off chance they collided to the same register,
because we are using the UNIQUE aggregator, it marked that register with -1.

Here is an example where we are storing the demographics of each item.

```
indexes {
  name: "Index"
  distribution {
    uniform {
      num_values: 10000000  # 10M
    }
  }
}
values {
  name: "SamplingIndicator"
  aggregator: UNIQUE
  distribution {
    uniform {
      num_values: 10000000  # 10M
    }
  }
}
values {
  name: "Demographics"
  aggregator: UNIQUE
  distribution {
    oracle {
      key: "demographics"
    }
  }
```

Create the sketch and insert elements to it

```
AnySketch anySketch = SketchProtos.toAnySketch(sketchConfig);
anySketch.insert(123456, ImmutableMap.of("demographics", 1L));
anySketch.insert(999999, ImmutableMap.of("demographics", 1L));
```

## Developer Guide

### Dependencies

*   [Bazel](https://bazel.build/)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md)

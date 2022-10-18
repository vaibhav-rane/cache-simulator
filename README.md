# Cache Simulator

Steps to execute the simulation

## 0. Requirements

The simulator is implemented with Java 8. Install [JDK](https://www.oracle.com/java/technologies/downloads/) to install java runtime environment.


## 1. Clean the compiled code

```bash
make clean
```

## 2. Compile

```bash
make
```

## 3. Run the simulator

```bash
java sim_cache <BLOCKSIZE> <L1_SIZE> <L1_ASSOC> <L2_SIZE> <L2_ASSOC> <REPLACEMENT_POLICY> <INCLUSION_PROPERTY> <trace_file>
```

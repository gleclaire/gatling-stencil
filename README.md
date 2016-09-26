Gatling Stencil
===============

Stencil repo that provides a bare bones Gradle project for running Gatling.

The following command demonstrates running a performance test against `http://myservice:8080`. The performance test will ramp from 1 user per second to 10 users per second over the course of a minute and then execute at a constant rate of 10 users per second for 5 minutes.

```
./gradlew :performance:gatling                       \
                -PrampUsersPerSecFrom=1              \
                -PrampUsersPerSecTo=10               \
                -PrampUsersPerSecDuring=1minute      \
                -PconstantUsersPerSec=10             \
                -PconstantUsersPerSecDuring=5minutes \
                -PtargetBaseUrl=http://myservice:8080
```

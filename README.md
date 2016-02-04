# Spring relaxed binding and decrypted values

Repo to show the problem reported [here](https://github.com/spring-cloud/spring-cloud-commons/issues/87)

## Context

Spring supports passing external values via environment variables and these can use a different naming syntax, e.g. `SERVICE_URL` instead of `service.url`.

References: [here](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-relaxed-binding) and [here](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-application-property-files)

I found this issue because we have a service and at deploy time we load the environment variables from a separate configuration repository. All sensitive configuration in this repository is encrypted, for some environments it is encrypted with keys that the team doesn't know. Then in the PaaS environment the app will have access to the [`ENCRYPT_KEY`](http://cloud.spring.io/spring-cloud-config/spring-cloud-config.html#_key_management) to be able to decrypt the configurations.

## Problem

When we try to mix Spring's relaxed binding and encrypted values it is not working as expected.

## Examples

### With encrypted environment variable using environment variable syntax for the property

It doesn't work as expected, the last line shows that the injected value for the property `foo.password` is the original encrypted value instead of the decrypted one.

```
$ ENCRYPT_KEY=foo FOO_PASSWORD={cipher}23cd3b5b92db6791c531fda2e2bdd51f2dd3d94d9f1de7b5ba932b5501dd72fc ./gradlew bootRun | grep -i foo
2016-02-03 17:37:52.113 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'FOO_PASSWORD' in [bootstrap]
2016-02-03 17:37:52.114 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'FOO_PASSWORD' in [decrypted]
2016-02-03 17:37:52.114 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'FOO_PASSWORD' in [decrypted] with type [String] and value 'mypassword'
2016-02-03 17:37:52.182 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'ENCRYPT_KEY' in [systemEnvironment] with type [String] and value 'foo'
2016-02-03 17:37:52.216 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'FOO_PASSWORD' in [bootstrap]
2016-02-03 17:37:52.216 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'FOO_PASSWORD' in [decrypted]
2016-02-03 17:37:52.216 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'FOO_PASSWORD' in [decrypted] with type [String] and value 'mypassword'
2016-02-03 17:37:52.224 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [bootstrap]
2016-02-03 17:37:52.224 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [decrypted]
2016-02-03 17:37:52.224 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [systemProperties]
2016-02-03 17:37:52.224 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [systemEnvironment]
2016-02-03 17:37:52.224 DEBUG 58198 --- [           main] o.s.c.e.SystemEnvironmentPropertySource  : PropertySource [systemEnvironment] does not contain 'foo.password', but found equivalent 'FOO_PASSWORD'
2016-02-03 17:37:52.225 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'foo.password' in [systemEnvironment] with type [String] and value '{cipher}23cd3b5b92db6791c531fda2e2bdd51f2dd3d94d9f1de7b5ba932b5501dd72fc'
2016-02-03 17:37:52.642 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [environmentProperties]
2016-02-03 17:37:52.642 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [bootstrap]
2016-02-03 17:37:52.642 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [decrypted]
2016-02-03 17:37:52.642 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [systemProperties]
2016-02-03 17:37:52.642 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [systemEnvironment]
2016-02-03 17:37:52.642 DEBUG 58198 --- [           main] o.s.c.e.SystemEnvironmentPropertySource  : PropertySource [systemEnvironment] does not contain 'foo.password', but found equivalent 'FOO_PASSWORD'
2016-02-03 17:37:52.642 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'foo.password' in [systemEnvironment] with type [String] and value '{cipher}23cd3b5b92db6791c531fda2e2bdd51f2dd3d94d9f1de7b5ba932b5501dd72fc'
2016-02-03 17:37:52.642 DEBUG 58198 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'foo.password' in [environmentProperties] with type [String] and value '{cipher}23cd3b5b92db6791c531fda2e2bdd51f2dd3d94d9f1de7b5ba932b5501dd72fc'
foo password is {cipher}23cd3b5b92db6791c531fda2e2bdd51f2dd3d94d9f1de7b5ba932b5501dd72fc
```

### With encrypted environment variable using dot-syntax for the property

It seems to work because we're passing the environment variable using the same syntax that the [`@Value`](https://github.com/tanob/spring-relaxed-binding-and-decrypted-values/blob/master/src/main/java/com/example/Foo.java#L12) declaration uses.

```
$ env ENCRYPT_KEY=foo foo.password={cipher}23cd3b5b92db6791c531fda2e2bdd51f2dd3d94d9f1de7b5ba932b5501dd72fc ./gradlew bootRun | grep -i foo
2016-02-03 17:41:03.134 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [bootstrap]
2016-02-03 17:41:03.135 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [decrypted]
2016-02-03 17:41:03.135 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'foo.password' in [decrypted] with type [String] and value 'mypassword'
2016-02-03 17:41:03.241 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'ENCRYPT_KEY' in [systemEnvironment] with type [String] and value 'foo'
2016-02-03 17:41:03.250 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [bootstrap]
2016-02-03 17:41:03.250 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [decrypted]
2016-02-03 17:41:03.250 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'foo.password' in [decrypted] with type [String] and value 'mypassword'
2016-02-03 17:41:03.256 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [bootstrap]
2016-02-03 17:41:03.256 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [decrypted]
2016-02-03 17:41:03.256 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'foo.password' in [decrypted] with type [String] and value 'mypassword'
2016-02-03 17:41:03.715 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [environmentProperties]
2016-02-03 17:41:03.715 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [bootstrap]
2016-02-03 17:41:03.715 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [decrypted]
2016-02-03 17:41:03.715 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'foo.password' in [decrypted] with type [String] and value 'mypassword'
2016-02-03 17:41:03.715 DEBUG 58307 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'foo.password' in [environmentProperties] with type [String] and value 'mypassword'
foo password is mypassword
```

### With non-encrypted environment variable using environment variable syntax for the property

```
$ FOO_PASSWORD='from env' ./gradlew bootRun | grep -i foo
2016-02-03 17:35:01.852 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'FOO_PASSWORD' in [bootstrap]
2016-02-03 17:35:01.852 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'FOO_PASSWORD' in [systemProperties]
2016-02-03 17:35:01.852 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'FOO_PASSWORD' in [systemEnvironment]
2016-02-03 17:35:01.852 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'FOO_PASSWORD' in [systemEnvironment] with type [String] and value 'from env'
2016-02-03 17:35:01.854 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [bootstrap]
2016-02-03 17:35:01.855 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [systemProperties]
2016-02-03 17:35:01.855 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [systemEnvironment]
2016-02-03 17:35:01.855 DEBUG 58163 --- [           main] o.s.c.e.SystemEnvironmentPropertySource  : PropertySource [systemEnvironment] does not contain 'foo.password', but found equivalent 'FOO_PASSWORD'
2016-02-03 17:35:01.856 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'foo.password' in [systemEnvironment] with type [String] and value 'from env'
2016-02-03 17:35:02.303 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [environmentProperties]
2016-02-03 17:35:02.304 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [bootstrap]
2016-02-03 17:35:02.304 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [systemProperties]
2016-02-03 17:35:02.304 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Searching for key 'foo.password' in [systemEnvironment]
2016-02-03 17:35:02.305 DEBUG 58163 --- [           main] o.s.c.e.SystemEnvironmentPropertySource  : PropertySource [systemEnvironment] does not contain 'foo.password', but found equivalent 'FOO_PASSWORD'
2016-02-03 17:35:02.305 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'foo.password' in [systemEnvironment] with type [String] and value 'from env'
2016-02-03 17:35:02.305 DEBUG 58163 --- [           main] o.s.c.e.PropertySourcesPropertyResolver  : Found key 'foo.password' in [environmentProperties] with type [String] and value 'from env'
foo password is from env
```

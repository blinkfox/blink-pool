# blink-pool

这是一个非常轻量级的、高性能的 JDBC 数据库连接池工具库，创建本项目的目的是用于原生支持 GraalVM 的 `native-image` 构建，非常适用于微应用、云原生等场景。

## 💎 一、特性

- 仅依赖 `slf4j`，非常的轻量级、高性能，且 jar 包仅 `22 KB`；
- 支持连接池基本的指标性能监控；
- 原生支持 GraalVM 的 `native-image` 构建；
- 须 Java 8 及以上，推荐用于微应用、云原生场景；

## 🌲 二、Maven 集成

```xml
<dependency>
    <groupId>com.blinkfox</groupId>
    <artifactId>blink-pool</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 🏝️ 三、使用示例

```java
BlinkDataSource dataSource = new BlinkDataSource(new BlinkConfig()
        .setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/postgres")
        .setUsername("sa")
        .setPassword("123456"));

try (Connection connection = dataSource.getConnection()) {
    // 使用数据库连接.
}
```

## ☘️ 四、SpringBoot 的集成

如果想在 SpringBoot 中集成 blink-pool，可以直接使用 `blink-pool-spring-boot-starter` 即可，不过需要排除掉 Spring Boot `JDBC` 或者 `JPA` 中默认的连接池。

```xml
<!-- 通常需要先排除掉 SpringBoot 中默认的连接池. -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <exclusions>
        <exclusion>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 再引入 blink-pool 的 starter 组件. -->
<dependency>
    <groupId>com.blinkfox</groupId>
    <artifactId>blink-pool-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 📝 五、开源许可证

本 blink-pool 库使用 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) 的开源许可证。

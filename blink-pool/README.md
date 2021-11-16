# blink-pool

这是一个非常轻量级的、高性能的 JDBC 数据库连接池工具库，创建本项目的目的是用于原生支持 GraalVM 的 `native-image` 构建，非常适用于微应用、云原生等场景。

## 💎 一、特性

- 仅依赖 `slf4j`，非常的轻量级、高性能，且 jar 包仅 `22 KB`；
- 支持连接池基本的指标性能监控；
- 原生支持 GraalVM 的 `native-image` 构建；
- 须 Java 8 及以上，推荐用于微应用、云原生场景；

## ☘️ 二、Maven 集成

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
    // 使用连接.
}
```

## 📝 四、开源许可证

本 blink-pool 库使用 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) 的开源许可证。

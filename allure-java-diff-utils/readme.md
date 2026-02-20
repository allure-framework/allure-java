## Allure-java-diff-utils
This library provides advanced data logging to allure when using [java-diff-utils](https://github.com/java-diff-utils/java-diff-utils)

## Wiki
https://java-diff-utils.github.io/java-diff-utils/

## Usage
Add dependency:
```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-java-diff-utils</artifactId>
    <version>LATEST</version>
    <scope>test</scope>
</dependency>
```

The library does not provide any ready-to-use assertions, but it does provide a method for generating an attachment 
to analyze the difference between left and right strings.
```java
    new AllureDiff().diff("my first line\nmy second line", "my first line");
```

Then an attachment will be generated in the Allure report to display the difference in the two lines.
Attachment example [here](examples/diff-two-lines.html)

# Examples
The library reveals itself to the maximum when comparing large structured data, such as json, csv or xml.

Json compare example:
```java
new AllureDiff().diff(
        """
        {
          "id": 123,
          "name": "John Doe",
          "email": "john.doe@example.com",
          "isActive": true,
          "roles": [
            "admin",
            "user"
          ],
          "address": {
            "street": "123 Main St",
            "city": "New York",
            "zipcode": "10001"
          }
        }
        """ ,
        """
        {
          "id": 123,
          "name": "John Doe",
          "email": "john.doe@example.com",
          "isActive": true,
          "roles": [
            "admin",
            "user"
          ],
          "address": {
            "city": "New York",
            "zipcode": "10001",
            "additional": "test"
          }
        }
        """
);
```
Attachment example [here](examples/diff-two-jsons.html)

Xml compare example:
```java
new AllureDiff().diff(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <user id="123">
            <name>John Doe</name>
            <email>john.doe@example.com</email>
            <isActive>true</isActive>
            <roles>
                <role>admin</role>
                <role>user</role>
            </roles>
            <address>
                <street>123 Main St</street>
                <city>New York</city>
                <zipcode>10001</zipcode>
            </address>
        </user>
        """,
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <user id="123">
            <name>John Doe</name>
            <email>john.doe@example.com</email>
            <isActive>true</isActive>
            <roles>
                <role>admin</role>
                <role>user</role>
            </roles>
            <address>
                <city>New York</city>
                <zipcode>10001</zipcode>
                <additional>test</additional>
            </address>
        </user>
        """
);
```
Attachment example [here](examples/diff-two-xmls.html)

## Assert
The library does not provide any assertions, but it does provide a Patch object that contains a diff that is available to any assertions needed.
```java
    final Patch<String> patch = new AllureDiff().diff("my first line\nmy second line", "my first line");
    assertThat(patch.getDeltas()).isEmpty();
```

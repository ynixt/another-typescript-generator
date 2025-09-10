Another Typescript Generator
====================

Another Typescript Generator is a Gradle plugin that generates Typescript interfaces from Kotlin/Java classes. Each class generates a new
file to prevent collision.

# Important notes

- This plugin reads the bytecode (class) and not the source. Do not forget to run `classes` command before.
- This plugin don't have yet any unit tests.

# Example

```kotlin
class Company(
    id: String? = null,
    val name: String,
    val money: BigDecimal,
    val imageUrl: String,
) : DatabaseEntity(
    id,
) {
    @ManyToOne(fetch = FetchType.LAZY)
    var owner: Player? = null
}
```

will generate

```typescript
import {DatabaseEntity} from './database-entity';
import {Player} from './player';

export interface Company extends DatabaseEntity {
    imageUrl: string;
    money: number;
    name: string;
    owner?: Player | null;
    id?: string | null;
}
```

# Using

## Installing

Add `id("io.github.ynixt.another-typescript-generator") version "1.2.0"` at plugins section

```kotlin
plugins {
    id("io.github.ynixt.another-typescript-generator") version "1.2.0"
}
```

## Default mappings

| Kotlin type                                                   | Typescript type      | Note                                                               |
|---------------------------------------------------------------|----------------------|--------------------------------------------------------------------|
| LocalDate, LocalDateTime, ZonedDateTime, Date                 | string               | Can be easily modified using the configuration parameter "mapDate" |
| String                                                        | string               |                                                                    |
| Byte, Short, Int, Long, Float, Double, BigDecimal, BigInteger | number               |                                                                    |
| Boolean                                                       | boolean              |                                                                    |
| Collection<T>                                                 | Array\<T>            |                                                                    |
| Map<K, T>                                                     | { [key: string]: T } |                                                                    |
| any other type                                                | any                  |                                                                    |

You can change or add any mapping using the configuration parameter `customTypes`.

## Configuring

Modify the task named `generateTypescriptInterfaces`. Here's a simple example:

```kotlin
tasks.named<GenerateTypescriptInterfacesTask>("generateTypescriptInterfaces") {
    outputPath = "./src/models/generated"
    classPackages =
        listOf(
            "io.github.ynixt.example",
        )
}
```

## Configuration parameters

### outputPath (Required)

Path, relative or absolute, that will placed the generated \*.ts files.

#### Example

```kotlin
outputPath = "./src/models/generated"
```

### classPackages

A permissive list of packages that contain the classes that will be used to generate the files.

Note: A class not listed here can be read if a listed class has a reference for it.

#### Example

```kotlin
classPackages = listOf(
    "io.github.ynixt.example",
)
```

### excludeClassPackages

A prohibitive list of packages. It takes priority over the permissive list.

Note: A class listed here can be read if a not listed class has a reference for it. If you really don't want this class use "ignoredClasses"
param

#### Example

```kotlin
excludeClassPackages = listOf(
    "io.github.ynixt.example",
)
```

### ignoredClasses

A list of classes that will not be read, no matter what. If there is any reference to it, this reference will be replaced by `any`.

#### Example

```kotlin
ignoredClasses = listOf(
    "io.github.ynixt.example.Book",
)
```

### ignoredFieldsByClass

A map of fields of classes that should be ignored.

#### Example

```kotlin
ignoredClasses = mutableMapOf(
    "io.github.ynixt.example.Person" to setOf("name", "age")
)
``` 

### mapDate

Defines which Typescript type should be used when a date type field is encountered (Date, LocalDate, LocalDateTime, ZonedDateTime).

| MapDateOption | Typescript type | Default |
|---------------|-----------------|---------|
| AS_STRING     | string          | X       |
| AS_LUXON      | DateTime        |         |
| AS_MOMENT     | moment.Moment   |         |
| AS_DATE       | Date            |         |
| AS_NUMBER     | number          |         |

#### Example

```kotlin
mapDate = MapDateOption.AS_LUXON
``` 

### customTypes

Add or replace the mapping between Typescript and Kotlin.

#### Example

```kotlin
 customTypes = listOf(
    CustomType(
        kotlin = AbsoluteKotlinType(String::class),
        typescript = AbsoluteTypescriptType(name = "any")
    ),
    CustomType(
        kotlin = AbsoluteKotlinTypeString("io.github.ynixt.entities.Book"),
        typescript = AbsoluteTypescriptType(name = "string", ignoreGenerics = true)
    )
)
``` 

### generateEnumOptions

Defines whether the enum should also generate an array containing all its options.

**Note:** default value is `true`.

#### Example

##### True

```kotlin
generateEnumOptions = true
``` 

for enum

```kotlin
enum class GroupPermissions {
    CHANGE_ROLE,
    ADD_MEMBER,
    REMOVE_MEMBER,

    SEND_ENTRIES,
}
```

generates

```typescript
export type GroupPermissions = 'CHANGE_ROLE' | 'ADD_MEMBER' | 'REMOVE_MEMBER' | 'SEND_ENTRIES';

export const GroupPermissions__Options: GroupPermissions[] = ['CHANGE_ROLE', 'ADD_MEMBER', 'REMOVE_MEMBER', 'SEND_ENTRIES'];
```

##### False

```kotlin
generateEnumOptions = false
``` 

for enum

```kotlin
enum class GroupPermissions {
    CHANGE_ROLE,
    ADD_MEMBER,
    REMOVE_MEMBER,

    SEND_ENTRIES,
}
```

generates

```typescript
export type GroupPermissions = 'CHANGE_ROLE' | 'ADD_MEMBER' | 'REMOVE_MEMBER' | 'SEND_ENTRIES';
```

### generateEnumObject

Defines whether the enum should also generate an object containing all its options.

**Note:** default value is `false`.

#### Example

##### True

```kotlin
generateEnumObject = true
``` 

for enum

```kotlin
enum class GroupPermissions {
    CHANGE_ROLE,
    ADD_MEMBER,
    REMOVE_MEMBER,

    SEND_ENTRIES,
}
```

generates

```typescript
export type GroupPermissions = 'CHANGE_ROLE' | 'ADD_MEMBER' | 'REMOVE_MEMBER' | 'SEND_ENTRIES';

export const GroupPermissions__Obj: { [K in GroupPermissions]: GroupPermissions } = {
    'CHANGE_ROLE': 'CHANGE_ROLE',
    'ADD_MEMBER': 'ADD_MEMBER',
    'REMOVE_MEMBER': 'REMOVE_MEMBER',
    'SEND_ENTRIES': 'SEND_ENTRIES',
};
```

##### False

```kotlin
generateEnumObject = false
``` 

for enum

```kotlin
enum class GroupPermissions {
    CHANGE_ROLE,
    ADD_MEMBER,
    REMOVE_MEMBER,

    SEND_ENTRIES,
}
```

generates

```typescript
export type GroupPermissions = 'CHANGE_ROLE' | 'ADD_MEMBER' | 'REMOVE_MEMBER' | 'SEND_ENTRIES';
```

# Development Log

Chronological record of the commands used to build GeoWarPlugin. Each section
maps a feature to the exact files it touched and the commit that recorded it.

## Build system

git add .gitignore .gitattributes build.gradle.kts gradlew gradlew.bat gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.jar
git commit -m "Fix build script and add Gradle wrapper"

## Role and permission model

git add src/main/java/com/geowar/model/role/NationPermission.java src/main/java/com/geowar/model/role/NationRole.java src/main/java/com/geowar/model/role/RolePermissionResolver.java
git commit -m "Add role and granular permission model"

## Core nation model

git add src/main/java/com/geowar/model/nation/ src/main/java/com/geowar/model/economy/ src/main/java/com/geowar/model/military/ src/main/java/com/geowar/model/town/
git commit -m "Add core nation domain model"

## Diplomacy and war model

git add src/main/java/com/geowar/model/diplomacy/ src/main/java/com/geowar/model/war/
git commit -m "Add diplomacy and war domain model"

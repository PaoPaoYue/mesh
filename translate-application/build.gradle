plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.4'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.github.paopaoyue'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '21'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-validation:3.2.3'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    annotationProcessor "org.springframework.boot:spring-boot-configuration-processor"
    implementation 'com.google.protobuf:protobuf-java:4.26.0'
    implementation project(':rpc')
}

tasks.named('test') {
    useJUnitPlatform()
}

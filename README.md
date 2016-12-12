## Digger Java client

A java integration library for AeroGear Digger

## Usage

Create job:

```
  DiggerClient client = DiggerClient.from("https://digger.com", "admin", "password");
  client.createJob("java-client-job1","https://github.com/wtrocki/helloworld-android-gradle","master");
```

Trigger a job:

```
  ...
  BuildStatus buildStatus = client.build("java-client-job1");
```

## Requirements

Client works with Java6 and above.

## Building

`mvn clean package`


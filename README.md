## Digger Java client

A java integration library for AeroGear Digger

## Usage

Build a default client:
```
   DiggerClient client = DiggerClient.createDefaultWithAuth("https://digger.com", "admin", "password");
```

Build a customized client:
```
   DiggerClient client = DiggerClient.builder()
         .createJobService(new CreateJobService())
         .triggerBuildService(new TriggerBuildService(10000, 100))
         .withAuth("https://digger.com", "admin", "password")
         .build();
```

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


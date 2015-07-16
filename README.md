## Fingerprint Worker를 위한 RHQ Plugin

### 소스코드 빌드

소스코드 빌드는 아래와 같이 Git을 이용하여 clone한 후 Apache Maven으로 빌드를 하도록 합니다.

```
# git clone URL
# cd rhq-fingerprint-worker-plugin
# mvn package
```

빌드 후에는 target 디렉토리에 rhq-fingerprint-worker-plugin-4.13.0.jar 파일이 생성되며 이 파일을 RHQ Server에서 로그인하여 Agent Plugin에 등록하도록 합니다.

### Challenge 12

For this challenge you will setup a Kubernetes environment, deploy a Spring application to it and monitor
it with Prometheus.

To begin, we will use the [Challenge 7](https://github.com/fat-potato-uk/rest-demo-2f) repository as 
a starting point for this exercise. As per the other challenges, you can clone this repository should you
wish for a completed solution.

If we are to deploy this application into a K8 environment it will need to be containerised. We can do
this by using the Dockerfile build approach outlined in [Cuke Docker Demo](https://github.com/fat-potato-uk/cuke-docker-demo):

```dockerfile
# ----------------------------------- Build phase ----------------------------------- #
ARG JAVA_VERSION=15
FROM maven:3.6-openjdk-${JAVA_VERSION} AS build

# Resolve the dependencies as an independent layer first
COPY pom.xml /usr/src/app/pom.xml
WORKDIR /usr/src/app
RUN mvn dependency:go-offline

# Copy and build
COPY src /usr/src/app/src
RUN mvn clean package

# ----------------------------------- Deployment phase ----------------------------------- #
# Move artifact into slim container
FROM openjdk:${JAVA_VERSION}-alpine
WORKDIR /usr/src/app
RUN apk add --update ttf-dejavu && rm -rf /var/cache/apk/*
COPY --from=build /usr/src/app/target/spring-helm-prom-1.0-SNAPSHOT.jar /usr/app/spring-helm-prom-1.0-SNAPSHOT.jar
ENTRYPOINT ["java","--enable-preview","-jar","/usr/app/spring-helm-prom-1.0-SNAPSHOT.jar"]
```

It's worth noting an important element in this Dockerfile, the leveraging of Docker layers for caching.
By pulling in the dependencies (`RUN mvn dependency:go-offline`) we can speed up subsequent builds as 
long as there has been no changes to the `pom.xml`.

To test this, we should build it! This can be done with the following command:

```bash
docker build -t spring-helm-prom:1.0 .
```

Next, we need a K8 environment. The easiest way for us to acquire one of these is to spin up a local
`minikube` instance. On MacOS, this can be achieved using `brew`:

```bash
brew install minikube
minikube config set driver docker
minikube start
minikube status
```

At this stage `minikube` should be up and running. You can view the dashboard through running the command:

```bash
minikube dashboard
```

This will open a browser window at the port forwarded address and present you with the K8 dashboard home
screen:

![K8](K8.png?raw=true "K8")

Once we have this, we want to install Prometheus for collecting our metrics. This can be done easily using the
public published Helm chart. For this, we will need to install Helm! For MacOS, again, `brew` works well for this:

```bash
brew install helm
```

Helm 3 (unlike 2) does not require any server side (K8) component to be present. As `minikube` automatically configures
your local K8 environment settings, everything should be ready to use:

```bash
ls -ltr ~/.kube/
total 32
-rw-r-----    1 andrew  staff   5788  4 Jun  2020 config.minikube
...
```
To install Prometheus, we will need to add the Helm repo and install the chart. At time of writing, [this is the location](https://github.com/prometheus-community/helm-charts)
for the current supported chart. It can be installed with:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update 
helm install prometheus prometheus-community/prometheus
```

If you now run `helm ls` you should see the installed chart:

```bash
$ helm ls
NAME      	NAMESPACE	REVISION	UPDATED                             	STATUS  	CHART            	APP VERSION
prometheus	default  	1       	2020-12-23 08:54:03.955539 +0000 UTC	deployed	prometheus-13.0.1	2.22.1     
```

As per the instructions in the install notes, you can view the GUI by running the following command:

```bash
export POD_NAME=$(kubectl get pods --namespace default -l "app=prometheus,component=server" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace default port-forward $POD_NAME 9090
```
(which simply forwards the exposed port for Prometheus to a local one) and visiting http://localhost:9090.

Now we want something to monitor. In order to do this we want to write a Helm chart for our application. The bulk of this
can be achieved by running the following command in the root directory:

```bash
helm create .
```
This will generate a lot of code, most of which is not necessary for our small example. For brevity however, will will 
only change what we need to deploy to our cluster:

`Chart.yaml`:
```yaml
- name: .
+ name: spring-helm-prom

- appVersion: 1.16.0
+ appVersion: "1.0"
```

`templates/_helpers.tpl` (append):
```yaml
{{- end -}}
{{/*
Annotations
*/}}
{{- define "..annotations" -}}
{{- if eq $.Values.prometheusScrape true -}}
prometheus.io/scrape: 'true'
prometheus.io/path: '/actuator/prometheus'
prometheus.io/port: '8080'
{{- end }}
{{- end -}}
```
This change allows us to add the additional annotation to the service so our pod is scraped automagically
by Prometheus.

`templates/deployment.yaml`:
```yaml
  ports:
    - name: http
-      containerPort: 80
+      containerPort: 8080
      protocol: TCP
  livenessProbe:
    httpGet:
-      path: /
+      path: /actuator/health
      port: http
  readinessProbe:
    httpGet:
-      path: /
+      path: /actuator/health
      port: http
```

`templates/service.yaml`:
```yaml

  name: {{ include "..fullname" . }}
  labels:
    {{- include "..labels" . | nindent 4 }}
+  annotations:
+    {{- include "..annotations" . | nindent 4 }}
spec:
  type: {{ .Values.service.type }}
  ports:
```

`values.yaml`:
```yaml
image:
-  repository: nginx
+  repository: spring-helm-prom
   pullPolicy: IfNotPresent

imagePullSecrets: []
- nameOverride: ""
- fullnameOverride: ""
+ nameOverride: "spring-helm-prom"
+ fullnameOverride: "spring-helm-prom"

+ prometheusScrape: true
```

With these change in place, you should now be able to deploy the chart:

```bash
helm install spring-helm-prom .
```

If you visit the K8 dashboard you should now see the following:

![K8-2](K8-2.png?raw=true "K8-2")

So what went wrong? If you click through you will find the following error listed in the pod events:

```text
Failed to pull image "spring-helm-prom:1.0": rpc error: code = Unknown desc = Error response from
daemon: pull access denied for spring-helm-prom, repository does not exist or may require 
'docker login': denied: requested access to the resource is denied
```

Translated, this is telling us that K8 couldn't find our container. Why is this? Well we built the container
in _our_ local Docker environment, not K8's. Usually we publish this containers to a centralised repository,
but as we are running this locally, the easiest thing to do is rebuilt the container in the K8 environment.

This can be done by running the following:

```bash
eval $(minikube docker-env)
```

If you now run `docker ls` you should find a lot of K8 related images in the list. 

To get our image in there, the easiest thing is just to rebuild it again:

```bash
docker build -t spring-helm-prom:1.0 .
```

K8 should now pick it up. 

If you visit the Prometheus GUI you can now see the metrics created in the application in the list:

![prom](prom.png?raw=true "prom")

If you don't see anything, it may be worth uninstalling and re-installing the chart:

```bash
helm delete spring-helm-prom
helm install spring-helm-prom .
```

If you want to increment these metric, you can port forward the service:

```bash
export POD_NAME=$(kubectl get pods --namespace default -l "app.kubernetes.io/name=spring-helm-prom,app.kubernetes.io/instance=spring-helm-prom" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace default port-forward $POD_NAME 8080:8080
```

And run the same commands as you did in previous challenges, e.g.

```bash
curl -v localhost:8080/employees
```

You should now see the metric increase in Prometheus!

Happy Helming!
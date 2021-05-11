# Istio Security

> https://istio.io/latest/docs/concepts/security/

microservices also have particular security needs:

- To defend against man-in-the-middle attacks, they need traffic encryption.

- To provide flexible service access control, they need mutual TLS and fine-grained access policies.
- To determine who did what at what time, they need auditing tools.

![Security overview](istio%20security.assets/overview.svg)

## High-level architecture

![Security Architecture](istio%20security.assets/arch-sec.svg)

## Istio identity

Identity is a fundamental concept of any security infrastructure. At the beginning of a workload-to-workload communication, the two parties must exchange credentials with their identity information for mutual authentication purposes.

The Istio identity model uses the first-class `service identity` to determine the identity of a request’s origin. This model allows for great flexibility and granularity for service identities to represent a human user, an individual workload, or a group of workloads. On platforms without a service identity, Istio can use other identities that can group workload instances, such as service names.

## Identity and certificate management

<img src="istio%20security.assets/id-prov.svg" alt="Identity Provisioning Workflow" style="zoom:200%;" />

Istio provisions keys and certificates through the following flow:

1. `istiod` offers a gRPC service to take [certificate signing requests](https://en.wikipedia.org/wiki/Certificate_signing_request) (CSRs).
2. When started, the Istio agent creates the private key and CSR, and then sends the CSR with its credentials to `istiod` for signing.
3. The CA in `istiod` validates the credentials carried in the CSR. Upon successful validation, it signs the CSR to generate the certificate.
4. When a workload is started, Envoy requests the certificate and key from the Istio agent in the same container via the [Envoy secret discovery service (SDS)](https://www.envoyproxy.io/docs/envoy/latest/configuration/security/secret#secret-discovery-service-sds) API.
5. The Istio agent sends the certificates received from `istiod` and the private key to Envoy via the Envoy SDS API.
6. Istio agent monitors the expiration of the workload certificate. The above process repeats periodically for certificate and key rotation.

## Authentication

Istio provides two types of authentication:

- Peer authentication: used for service-to-service authentication to verify the client making the connection. Istio offers [mutual TLS](https://en.wikipedia.org/wiki/Mutual_authentication) as a full stack solution for transport authentication, which can be enabled without requiring service code changes. 
- Request authentication: Used for end-user authentication to verify the credential attached to the request. Istio enables request-level authentication with JSON Web Token (JWT) validation and a streamlined developer experience using a custom authentication provider or any OpenID Connect providers, for example:
  - [ORY Hydra](https://www.ory.sh/)
  - [Keycloak](https://www.keycloak.org/)
  - [Auth0](https://auth0.com/)
  - [Firebase Auth](https://firebase.google.com/docs/auth/)
  - [Google Auth](https://developers.google.com/identity/protocols/OpenIDConnect)

In all cases, Istio stores the authentication policies in the `Istio config store` via a custom Kubernetes API. Istiod keeps them up-to-date for each proxy, along with the keys where appropriate. 

### Mutual TLS authentication

Istio tunnels service-to-service communication through the client- and server-side PEPs, which are implemented as [Envoy proxies](https://envoyproxy.github.io/envoy/). When a workload sends a request to another workload using mutual TLS authentication, the request is handled as follows:

1. Istio re-routes the outbound traffic from a client to the client’s local sidecar Envoy.
2. The client side Envoy starts a mutual TLS handshake with the server side Envoy. During the handshake, the client side Envoy also does a [secure naming](https://istio.io/latest/docs/concepts/security/#secure-naming) check to verify that the service account presented in the server certificate is authorized to run the target service.
3. The client side Envoy and the server side Envoy establish a mutual TLS connection, and Istio forwards the traffic from the client side Envoy to the server side Envoy.
4. The server side Envoy authorizes the request. If authorized, it forwards the traffic to the backend service through local TCP connections.

Istio configures `TLSv1_2` as the minimum TLS version for both client and server with the following cipher suites:

- `CDHE-ECDSA-AES256-GCM-SHA384`
- `ECDHE-RSA-AES256-GCM-SHA384`
- `ECDHE-ECDSA-AES128-GCM-SHA256`
- `ECDHE-RSA-AES128-GCM-SHA256`
- `AES256-GCM-SHA384`
- `AES128-GCM-SHA256`

#### Permissive mode

Istio mutual TLS has a permissive mode, which allows a service to accept both plaintext traffic and mutual TLS traffic at the same time. This feature greatly improves the mutual TLS onboarding experience.

#### Secure naming

Server identities are encoded in certificates, but service names are retrieved through the discovery service or DNS. The secure naming information maps the server identities to the service names. A mapping of identity `A` to service name `B` means “`A` is authorized to run service `B`”. The control plane watches the `apiserver`, generates the secure naming mappings, and distributes them securely to the PEPs. 

### Authentication architecture

Client services, those that send requests, are responsible for following the necessary authentication mechanism. For request authentication, the application is responsible for acquiring and attaching the JWT credential to the request. For peer authentication, Istio automatically upgrades all traffic between two PEPs to mutual TLS. 

![Authentication Architecture](istio%20security.assets/authn.svg)

### Authentication policies

Authentication policies apply to requests that a service receives.

#### Policy storage

Istio stores mesh-scope policies in the root namespace. These policies have an empty selector apply to all workloads in the mesh. Policies that have a namespace scope are stored in the corresponding namespace. They only apply to workloads within their namespace. If you configure a `selector` field, the authentication policy only applies to workloads matching the conditions you configured.

Peer and request authentication policies are stored separately by kind, `PeerAuthentication` and `RequestAuthentication` respectively.

There can be only one mesh-wide peer authentication policy, and only one namespace-wide peer authentication policy per namespace. When you configure multiple mesh- or namespace-wide peer authentication policies for the same mesh or namespace, Istio ignores the newer policies. When more than one workload-specific peer authentication policy matches, Istio picks the oldest one.

Istio applies the narrowest matching policy for each workload using the following order:

1. workload-specific
2. namespace-wide
3. mesh-wide

Istio can combine all matching request authentication policies to work as if they come from a single request authentication policy. Thus, you can have multiple mesh-wide or namespace-wide policies in a mesh or namespace. However, it is still a good practice to avoid having multiple mesh-wide or namespace-wide request authentication policies.

#### Peer authentication

Peer authentication policies specify the mutual TLS mode Istio enforces on target workloads. The following modes are supported:

- PERMISSIVE: Workloads accept both mutual TLS and plain text traffic. This mode is most useful during migrations when workloads without sidecar cannot use mutual TLS. Once workloads are migrated with sidecar injection, you should switch the mode to STRICT.
- STRICT: Workloads only accept mutual TLS traffic.
- DISABLE: Mutual TLS is disabled. From a security perspective, you shouldn’t use this mode unless you provide your own security solution.

#### Request authentication

Request authentication policies specify the values needed to validate a JSON Web Token (JWT). These values include, among others, the following:

- The location of the token in the request
- The issuer or the request
- The public JSON Web Key Set (JWKS)

Istio checks the presented token, if presented against the rules in the request authentication policy, and rejects requests with invalid tokens. When requests carry no token, they are accepted by default. To reject requests without tokens, provide authorization rules that specify the restrictions for specific operations, for example paths or actions.

Request authentication policies can specify more than one JWT if each uses a unique location. When more than one policy matches a workload, Istio combines all rules as if they were specified as a single policy. This behavior is useful to program workloads to accept JWT from different providers. However, requests with more than one valid JWT are not supported because the output principal of such requests is undefined.

#### Principals

When you use peer authentication policies and mutual TLS, Istio extracts the identity from the peer authentication into the `source.principal`. Similarly, when you use request authentication policies, Istio assigns the identity from the JWT to the `request.auth.principal`. Use these principals to set authorization policies and as telemetry output.

## Authorization

Istio’s authorization features provide mesh-, namespace-, and workload-wide access control for your workloads in the mesh. This level of control provides the following benefits:

- Workload-to-workload and end-user-to-workload authorization.
- A Simple API: it includes a single [`AuthorizationPolicy` CRD](https://istio.io/latest/docs/reference/config/security/authorization-policy/), which is easy to use and maintain.
- Flexible semantics: operators can define custom conditions on Istio attributes, and use DENY and ALLOW actions.
- High performance: Istio authorization is enforced natively on Envoy.
- High compatibility: supports gRPC, HTTP, HTTPS and HTTP2 natively, as well as any plain TCP protocols.

### Authorization architecture

![Authorization Architecture](istio%20security.assets/authz.svg)

### Implicit enablement

You don’t need to explicitly enable Istio’s authorization features. Just apply an authorization policy to the workloads to enforce access control. For workloads without authorization policies applied, Istio doesn’t enforce access control allowing all requests

![Authorization Policy Precedence](istio%20security.assets/authz-eval.png)

### Authorization policies

To configure an authorization policy, you create an [`AuthorizationPolicy` custom resource](https://istio.io/latest/docs/reference/config/security/authorization-policy/). An authorization policy includes a selector, an action, and a list of rules:

- The `selector` field specifies the target of the policy
- The `action` field specifies whether to allow or deny the request
- The `rules` specify when to trigger the action
  - The `from` field in the `rules` specifies the sources of the request
  - The `to` field in the `rules` specifies the operations of the request
  - The `when` field specifies the conditions needed to apply the rule


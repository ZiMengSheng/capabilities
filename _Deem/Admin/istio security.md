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
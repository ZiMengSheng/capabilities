# Kubernetes API 访问控制

本页面概述了对 Kubernetes API 的访问控制。

用户使用 `kubectl`、客户端库或构造 REST 请求来访问 [Kubernetes API](https://kubernetes.io/zh/docs/concepts/overview/kubernetes-api/)。 人类用户和 [Kubernetes 服务账户](https://kubernetes.io/zh/docs/tasks/configure-pod-container/configure-service-account/)都可以被鉴权访问 API。 当请求到达 API 时，它会经历多个阶段，如下图所示：

![Kubernetes API 请求处理步骤示意图](Kubernetes%20API%20%E8%AE%BF%E9%97%AE%E6%8E%A7%E5%88%B6.assets/access-control-overview.svg)

## 传输安全

在典型的 Kubernetes 集群中，API 服务器在 443 端口上提供服务，受 TLS 保护。 API 服务器出示证书。 该证书可以使用私有证书颁发机构（CA）签名，也可以基于链接到公认的 CA 的公钥基础架构签名。

如果你的集群使用私有证书颁发机构，你需要在客户端的 `~/.kube/config` 文件中提供该 CA 证书的副本， 以便你可以信任该连接并确认该连接没有被拦截。

你的客户端可以在此阶段出示 TLS 客户端证书。

## 认证

如上图步骤 **1** 所示，建立 TLS 后， HTTP 请求将进入认证（Authentication）步骤。 集群创建脚本或者集群管理员配置 API 服务器，使之运行一个或多个身份认证组件。 身份认证组件在[认证](https://kubernetes.io/zh/docs/reference/access-authn-authz/authentication/)节中有更详细的描述。

认证步骤的输入整个 HTTP 请求；但是，通常组件只检查头部或/和客户端证书。

认证模块包含客户端证书、密码、普通令牌、引导令牌和 JSON Web 令牌（JWT，用于服务账户）。

可以指定多个认证模块，在这种情况下，服务器依次尝试每个验证模块，直到其中一个成功。

如果请求认证不通过，服务器将以 HTTP 状态码 401 拒绝该请求。 反之，该用户被认证为特定的 `username`，并且该用户名可用于后续步骤以在其决策中使用。 部分验证器还提供用户的组成员身份，其他则不提供。

## 鉴权

如上图的步骤 **2** 所示，将请求验证为来自特定的用户后，请求必须被鉴权。

请求必须包含请求者的用户名、请求的行为以及受该操作影响的对象。 如果现有策略声明用户有权完成请求的操作，那么该请求被鉴权通过。

例如，如果 Bob 有以下策略，那么他只能在 `projectCaribou` 名称空间中读取 Pod。

```json
{
    "apiVersion": "abac.authorization.kubernetes.io/v1beta1",
    "kind": "Policy",
    "spec": {
        "user": "bob",
        "namespace": "projectCaribou",
        "resource": "pods",
        "readonly": true
    }
}
```

如果 Bob 执行以下请求，那么请求会被鉴权，因为允许他读取 `projectCaribou` 名称空间中的对象。

```json
{
  "apiVersion": "authorization.k8s.io/v1beta1",
  "kind": "SubjectAccessReview",
  "spec": {
    "resourceAttributes": {
      "namespace": "projectCaribou",
      "verb": "get",
      "group": "unicorn.example.org",
      "resource": "pods"
    }
  }
}
```

如果 Bob 在 `projectCaribou` 名字空间中请求写（`create` 或 `update`）对象，其鉴权请求将被拒绝。 如果 Bob 在诸如 `projectFish` 这类其它名字空间中请求读取（`get`）对象，其鉴权也会被拒绝。

Kubernetes 鉴权要求使用公共 REST 属性与现有的组织范围或云提供商范围的访问控制系统进行交互。 使用 REST 格式很重要，因为这些控制系统可能会与 Kubernetes API 之外的 API 交互。

Kubernetes 支持多种鉴权模块，例如 ABAC 模式、RBAC 模式和 Webhook 模式等。 管理员创建集群时，他们配置应在 API 服务器中使用的鉴权模块。 如果配置了多个鉴权模块，则 Kubernetes 会检查每个模块，任意一个模块鉴权该请求，请求即可继续； 如果所有模块拒绝了该请求，请求将会被拒绝（HTTP 状态码 403）。

要了解更多有关 Kubernetes 鉴权的更多信息，包括有关使用支持鉴权模块创建策略的详细信息， 请参阅[鉴权](https://kubernetes.io/zh/docs/reference/access-authn-authz/authorization/)。

## 准入控制

准入控制模块是可以修改或拒绝请求的软件模块。 除鉴权模块可用的属性外，准入控制模块还可以访问正在创建或修改的对象的内容。

准入控制器对创建、修改、删除或（通过代理）连接对象的请求进行操作。 准入控制器不会对仅读取对象的请求起作用。 有多个准入控制器被配置时，服务器将依次调用它们。

这一操作如上图的步骤 **3** 所示。

与身份认证和鉴权模块不同，如果任何准入控制器模块拒绝某请求，则该请求将立即被拒绝。

除了拒绝对象之外，准入控制器还可以为字段设置复杂的默认值。

可用的准入控制模块在[准入控制器](https://kubernetes.io/zh/docs/reference/access-authn-authz/admission-controllers/)中进行了描述。

请求通过所有准入控制器后，将使用检验例程检查对应的 API 对象，然后将其写入对象存储（如步骤 **4** 所示）。

## API 服务器端口和 IP

前面的讨论适用于发送到 API 服务器的安全端口的请求（典型情况）。 API 服务器实际上可以在 2 个端口上提供服务：

默认情况下，Kubernetes API 服务器在 2 个端口上提供 HTTP 服务：

1. `localhost` 端口:
   - 用于测试和引导，以及主控节点上的其他组件（调度器，控制器管理器）与 API 通信
   - 没有 TLS
   - 默认为端口 8080，使用 `--insecure-port` 进行更改
   - 默认 IP 为 localhost，使用 `--insecure-bind-address` 进行更改
   - 请求 **绕过** 身份认证和鉴权模块
   - 由准入控制模块处理的请求
   - 受需要访问主机的保护
2. “安全端口”：
   - 尽可能使用
   - 使用 TLS。 用 `--tls-cert-file` 设置证书，用 `--tls-private-key-file` 设置密钥
   - 默认端口 6443，使用 `--secure-port` 更改
   - 默认 IP 是第一个非本地网络接口，使用 `--bind-address` 更改
   - 请求须经身份认证和鉴权组件处理
   - 请求须经准入控制模块处理
   - 身份认证和鉴权模块运行

## 接下来

阅读更多有关身份认证、鉴权和 API 访问控制的文档：

- 认证
  - [使用 Bootstrap 令牌进行身份认证](https://kubernetes.io/zh/docs/reference/access-authn-authz/bootstrap-tokens/)
- 准入控制器
  - [动态准入控制](https://kubernetes.io/zh/docs/reference/access-authn-authz/extensible-admission-controllers/)
- 鉴权
  - [基于角色的访问控制](https://kubernetes.io/zh/docs/reference/access-authn-authz/rbac/)
  - [基于属性的访问控制](https://kubernetes.io/zh/docs/reference/access-authn-authz/abac/)
  - [节点鉴权](https://kubernetes.io/zh/docs/reference/access-authn-authz/node/)
  - [Webhook 鉴权](https://kubernetes.io/zh/docs/reference/access-authn-authz/webhook/)
- 证书签名请求
  - 包括 [CSR 认证](https://kubernetes.io/zh/docs/reference/access-authn-authz/certificate-signing-requests/#approval-rejection) 和[证书签名](https://kubernetes.io/zh/docs/reference/access-authn-authz/certificate-signing-requests/#signing)
- 服务账户
  - [开发者指导](https://kubernetes.io/zh/docs/tasks/configure-pod-container/configure-service-account/)
  - [管理](https://kubernetes.io/zh/docs/reference/access-authn-authz/service-accounts-admin/)

你可以了解

- Pod 如何使用 [Secrets](https://kubernetes.io/zh/docs/concepts/configuration/secret/#service-accounts-automatically-create-and-attach-secrets-with-api-credentials) 获取 API 凭证.
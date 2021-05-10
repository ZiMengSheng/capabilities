# 使用 RBAC 鉴权

基于角色（Role）的访问控制（RBAC）是一种基于组织中用户的角色来调节控制对 计算机或网络资源的访问的方法。

RBAC 鉴权机制使用 `rbac.authorization.k8s.io` [API 组](https://kubernetes.io/zh/docs/concepts/overview/kubernetes-api/#api-groups) 来驱动鉴权决定，允许你通过 Kubernetes API 动态配置策略。

要启用 RBAC，在启动 [API 服务器](https://kubernetes.io/zh/docs/reference/command-line-tools-reference/kube-apiserver/) 时将 `--authorization-mode` 参数设置为一个逗号分隔的列表并确保其中包含 `RBAC`。

```shell
kube-apiserver --authorization-mode=Example,RBAC --<其他选项> --<其他选项>
```

## API 对象

RBAC API 声明了四种 Kubernetes 对象：*Role*、*ClusterRole*、*RoleBinding* 和 *ClusterRoleBinding*。你可以像使用其他 Kubernetes 对象一样， 通过类似 `kubectl` 这类工具 [描述对象](https://kubernetes.io/zh/docs/concepts/overview/working-with-objects/kubernetes-objects/#understanding-kubernetes-objects), 或修补对象。

> **注意：**
>
> 这些对象在设计时即实施了一些访问限制。如果你在学习过程中对集群做了更改，请参考 [避免特权提升和引导](https://kubernetes.io/zh/docs/reference/access-authn-authz/rbac/#privilege-escalation-prevention-and-bootstrapping) 一节，以了解这些限制会以怎样的方式阻止你做出修改。

### Role 和 ClusterRole

RBAC 的 *Role* 或 *ClusterRole* 中包含一组代表相关权限的规则。 这些权限是纯粹累加的（不存在拒绝某操作的规则）。

Role 总是用来在某个[名字空间](https://kubernetes.io/zh/docs/concepts/overview/working-with-objects/namespaces/) 内设置访问权限；在你创建 Role 时，你必须指定该 Role 所属的名字空间。

与之相对，ClusterRole 则是一个集群作用域的资源。这两种资源的名字不同（Role 和 ClusterRole）是因为 Kubernetes 对象要么是名字空间作用域的，要么是集群作用域的， 不可两者兼具。

ClusterRole 有若干用法。你可以用它来：

1. 定义对某名字空间域对象的访问权限，并将在各个名字空间内完成授权；
2. 为名字空间作用域的对象设置访问权限，并跨所有名字空间执行授权；
3. 为集群作用域的资源定义访问权限。

如果你希望在名字空间内定义角色，应该使用 Role； 如果你希望定义集群范围的角色，应该使用 ClusterRole。

#### Role 示例

下面是一个位于 "default" 名字空间的 Role 的示例，可用来授予对 [pods](https://kubernetes.io/docs/concepts/workloads/pods/pod-overview/) 的读访问权限：

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: default
  name: pod-reader
rules:
- apiGroups: [""] # "" 标明 core API 组
  resources: ["pods"]
  verbs: ["get", "watch", "list"]
```

### ClusterRole 示例

ClusterRole 可以和 Role 相同完成授权。 因为 ClusterRole 属于集群范围，所以它也可以为以下资源授予访问权限：

- 集群范围资源（比如 [节点（Node）](https://kubernetes.io/zh/docs/concepts/architecture/nodes/)）
- 非资源端点（比如 `/healthz`）
- 跨名字空间访问的名字空间作用域的资源（如 Pods），比如，你可以使用 ClusterRole 来允许某特定用户执行 `kubectl get pods --all-namespaces`

下面是一个 ClusterRole 的示例，可用来为任一特定名字空间中的 [Secret](https://kubernetes.io/zh/docs/concepts/configuration/secret/) 授予读访问权限， 或者跨名字空间的访问权限（取决于该角色是如何[绑定](https://kubernetes.io/zh/docs/reference/access-authn-authz/rbac/#rolebinding-and-clusterrolebinding)的）：

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  # "namespace" 被忽略，因为 ClusterRoles 不受名字空间限制
  name: secret-reader
rules:
- apiGroups: [""]
  # 在 HTTP 层面，用来访问 Secret 对象的资源的名称为 "secrets"
  resources: ["secrets"]
  verbs: ["get", "watch", "list"]
```

Role 或 ClusterRole 对象的名称必须是合法的 [路径区段名称](https://kubernetes.io/zh/docs/concepts/overview/working-with-objects/names#path-segment-names)。

### RoleBinding 和 ClusterRoleBinding

角色绑定（Role Binding）是将角色中定义的权限赋予一个或者一组用户。 它包含若干 **主体**（用户、组或服务账户）的列表和对这些主体所获得的角色的引用。 RoleBinding 在指定的名字空间中执行授权，而 ClusterRoleBinding 在集群范围执行授权。

一个 RoleBinding 可以引用同一的名字空间中的任何 Role。 或者，一个 RoleBinding 可以引用某 ClusterRole 并将该 ClusterRole 绑定到 RoleBinding 所在的名字空间。 如果你希望将某 ClusterRole 绑定到集群中所有名字空间，你要使用 ClusterRoleBinding。

RoleBinding 或 ClusterRoleBinding 对象的名称必须是合法的 [路径区段名称](https://kubernetes.io/zh/docs/concepts/overview/working-with-objects/names#path-segment-names)。

#### RoleBinding 示例

下面的例子中的 RoleBinding 将 "pod-reader" Role 授予在 "default" 名字空间中的用户 "jane"。 这样，用户 "jane" 就具有了读取 "default" 名字空间中 pods 的权限。

```yaml
apiVersion: rbac.authorization.k8s.io/v1
# 此角色绑定允许 "jane" 读取 "default" 名字空间中的 Pods
kind: RoleBinding
metadata:
  name: read-pods
  namespace: default
subjects:
# 你可以指定不止一个“subject（主体）”
- kind: User
  name: jane # "name" 是不区分大小写的
  apiGroup: rbac.authorization.k8s.io
roleRef:
  # "roleRef" 指定与某 Role 或 ClusterRole 的绑定关系
  kind: Role # 此字段必须是 Role 或 ClusterRole
  name: pod-reader     # 此字段必须与你要绑定的 Role 或 ClusterRole 的名称匹配
  apiGroup: rbac.authorization.k8s.io
```

RoleBinding 也可以引用 ClusterRole，以将对应 ClusterRole 中定义的访问权限授予 RoleBinding 所在名字空间的资源。这种引用使得你可以跨整个集群定义一组通用的角色， 之后在多个名字空间中复用。

例如，尽管下面的 RoleBinding 引用的是一个 ClusterRole，"dave"（这里的主体， 不区分大小写）只能访问 "development" 名字空间中的 Secrets 对象，因为 RoleBinding 所在的名字空间（由其 metadata 决定）是 "development"。

```yaml
apiVersion: rbac.authorization.k8s.io/v1
# 此角色绑定使得用户 "dave" 能够读取 "default" 名字空间中的 Secrets
# 你需要一个名为 "secret-reader" 的 ClusterRole
kind: RoleBinding
metadata:
  name: read-secrets
  # RoleBinding 的名字空间决定了访问权限的授予范围。
  # 这里仅授权在 "development" 名字空间内的访问权限。
  namespace: development
subjects:
- kind: User
  name: dave # 'name' 是不区分大小写的
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: secret-reader
  apiGroup: rbac.authorization.k8s.io
```

#### ClusterRoleBinding 示例

要跨整个集群完成访问权限的授予，你可以使用一个 ClusterRoleBinding。 下面的 ClusterRoleBinding 允许 "manager" 组内的所有用户访问任何名字空间中的 Secrets。

```yaml
apiVersion: rbac.authorization.k8s.io/v1
# 此集群角色绑定允许 “manager” 组中的任何人访问任何名字空间中的 secrets
kind: ClusterRoleBinding
metadata:
  name: read-secrets-global
subjects:
- kind: Group
  name: manager # 'name' 是不区分大小写的
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: secret-reader
  apiGroup: rbac.authorization.k8s.io
```

创建了绑定之后，你不能再修改绑定对象所引用的 Role 或 ClusterRole。 试图改变绑定对象的 `roleRef` 将导致合法性检查错误。 如果你想要改变现有绑定对象中 `roleRef` 字段的内容，必须删除重新创建绑定对象。

这种限制有两个主要原因：

1. 针对不同角色的绑定是完全不一样的绑定。要求通过删除/重建绑定来更改 `roleRef`, 这样可以确保要赋予绑定的所有主体会被授予新的角色（而不是在允许修改 `roleRef` 的情况下导致所有现有主体未经验证即被授予新角色对应的权限）。
2. 将 `roleRef` 设置为不可以改变，这使得可以为用户授予对现有绑定对象的 `update` 权限， 这样可以让他们管理主体列表，同时不能更改被授予这些主体的角色。

命令 `kubectl auth reconcile` 可以创建或者更新包含 RBAC 对象的清单文件， 并且在必要的情况下删除和重新创建绑定对象，以改变所引用的角色。 更多相关信息请参照[命令用法和示例](https://kubernetes.io/zh/docs/reference/access-authn-authz/rbac/#kubectl-auth-reconcile)

### 对资源的引用

在 Kubernetes API 中，大多数资源都是使用对象名称的字符串表示来呈现与访问的。 例如，对于 Pod 应使用 "pods"。 RBAC 使用对应 API 端点的 URL 中呈现的名字来引用资源。 有一些 Kubernetes API 涉及 **子资源（subresource）**，例如 Pod 的日志。 对 Pod 日志的请求看起来像这样：

```http
GET /api/v1/namespaces/{namespace}/pods/{name}/log
```

在这里，`pods` 对应名字空间作用域的 Pod 资源，而 `log` 是 `pods` 的子资源。 在 RBAC 角色表达子资源时，使用斜线（`/`）来分隔资源和子资源。 要允许某主体读取 `pods` 同时访问这些 Pod 的 `log` 子资源，你可以这么写：

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: default
  name: pod-and-pod-logs-reader
rules:
- apiGroups: [""]
  resources: ["pods", "pods/log"]
  verbs: ["get", "list"]
```

对于某些请求，也可以通过 `resourceNames` 列表按名称引用资源。 在指定时，可以将请求限定为资源的单个实例。 下面的例子中限制可以 "get" 和 "update" 一个名为 `my-configmap` 的 [ConfigMap](https://kubernetes.io/zh/docs/tasks/configure-pod-container/configure-pod-configmap/)：

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: default
  name: configmap-updater
rules:
- apiGroups: [""]
  # 在 HTTP 层面，用来访问 ConfigMap 的资源的名称为 "configmaps"
  resources: ["configmaps"]
  resourceNames: ["my-configmap"]
  verbs: ["update", "get"]
```

> **说明：**
>
> 你不能针对 `create` 或者 `deletecollection` 请求来实施 resourceName 限制。 对于 `create` 操作而言，这是因为在鉴权时还不知道对象名称。

### 聚合的 ClusterRole

你可以将若干 ClusterRole **聚合（Aggregate）** 起来，形成一个复合的 ClusterRole。 某个控制器作为集群控制面的一部分会监视带有 `aggregationRule` 的 ClusterRole 对象集合。`aggregationRule` 为控制器定义一个标签 [选择算符](https://kubernetes.io/zh/docs/concepts/overview/working-with-objects/labels/)供后者匹配 应该组合到当前 ClusterRole 的 `roles` 字段中的 ClusterRole 对象。

下面是一个聚合 ClusterRole 的示例：

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: monitoring
aggregationRule:
  clusterRoleSelectors:
  - matchLabels:
      rbac.example.com/aggregate-to-monitoring: "true"
rules: [] # 控制面自动填充这里的规则
```

如果你创建一个与某现有聚合 ClusterRole 的标签选择算符匹配的 ClusterRole， 这一变化会触发新的规则被添加到聚合 ClusterRole 的操作。 下面的例子中，通过创建一个标签同样为 `rbac.example.com/aggregate-to-monitoring: true` 的 ClusterRole，新的规则可被添加到 "monitoring" ClusterRole 中。

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: monitoring-endpoints
  labels:
    rbac.example.com/aggregate-to-monitoring: "true"
# 当你创建 "monitoring-endpoints" ClusterRole 时，
# 下面的规则会被添加到 "monitoring" ClusterRole 中
rules:
- apiGroups: [""]
  resources: ["services", "endpoints", "pods"]
  verbs: ["get", "list", "watch"]
```

默认的[面向用户的角色](https://kubernetes.io/zh/docs/reference/access-authn-authz/rbac/#default-roles-and-role-bindings) 使用 ClusterRole 聚合。 这使得作为集群管理员的你可以为扩展默认规则，包括为定制资源设置规则， 比如通过 CustomResourceDefinitions 或聚合 API 服务器提供的定制资源。

例如，下面的 ClusterRoles 让默认角色 "admin" 和 "edit" 拥有管理自定义资源 "CronTabs" 的权限， "view" 角色对 CronTab 资源拥有读操作权限。 你可以假定 CronTab 对象在 API 服务器所看到的 URL 中被命名为 `"crontabs"`。

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: aggregate-cron-tabs-edit
  labels:
    # 添加以下权限到默认角色 "admin" 和 "edit" 中
    rbac.authorization.k8s.io/aggregate-to-admin: "true"
    rbac.authorization.k8s.io/aggregate-to-edit: "true"
rules:
- apiGroups: ["stable.example.com"]
  resources: ["crontabs"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
---
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: aggregate-cron-tabs-view
  labels:
    # 添加以下权限到 "view" 默认角色中
    rbac.authorization.k8s.io/aggregate-to-view: "true"
rules:
- apiGroups: ["stable.example.com"]
  resources: ["crontabs"]
  verbs: ["get", "list", "watch"]
```

#### Role 示例

以下示例均为从 Role 或 CLusterRole 对象中截取出来，我们仅展示其 `rules` 部分。

允许读取在核心 [API 组](https://kubernetes.io/zh/docs/concepts/overview/kubernetes-api/#api-groups)下的 `"Pods"`：

```yaml
rules:
- apiGroups: [""]
  # 在 HTTP 层面，用来访问 Pod 的资源的名称为 "pods"
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
```

允许读/写在 "extensions" 和 "apps" API 组中的 Deployment（在 HTTP 层面，对应 URL 中资源部分为 "deployments"）：

```yaml
rules:
- apiGroups: ["extensions", "apps"]
  resources: ["deployments"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
```

允许读取核心 API 组中的 "pods" 和读/写 `"batch"` 或 `"extensions"` API 组中的 "jobs"：

```yaml
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["batch", "extensions"]
  resources: ["jobs"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
```

允许读取名称为 "my-config" 的 ConfigMap（需要通过 RoleBinding 绑定以 限制为某名字空间中特定的 ConfigMap）：

```yaml
rules:
- apiGroups: [""]
  resources: ["configmaps"]
  resourceNames: ["my-config"]
  verbs: ["get"]
```

允许读取在核心组中的 "nodes" 资源（因为 `Node` 是集群作用域的，所以需要 ClusterRole 绑定到 ClusterRoleBinding 才生效）：

```yaml
rules:
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["get", "list", "watch"]
```

允许针对非资源端点 `/healthz` 和其子路径上发起 GET 和 POST 请求 （必须在 ClusterRole 绑定 ClusterRoleBinding 才生效）：

```yaml
rules:
  - nonResourceURLs: ["/healthz", "/healthz/*"] # nonResourceURL 中的 '*' 是一个全局通配符
    verbs: ["get", "post"]
```

### 对主体的引用

RoleBinding 或者 ClusterRoleBinding 可绑定角色到某 *主体（Subject）*上。 主体可以是组，用户或者 [服务账户](https://kubernetes.io/zh/docs/tasks/configure-pod-container/configure-service-account/)。

Kubernetes 用字符串来表示用户名。 用户名可以是普通的用户名，像 "alice"；或者是邮件风格的名称，如 "bob@example.com"， 或者是以字符串形式表达的数字 ID。 你作为 Kubernetes 管理员负责配置 [身份认证模块](https://kubernetes.io/zh/docs/reference/access-authn-authz/authentication/) 以便后者能够生成你所期望的格式的用户名。

> **注意：**
>
> 前缀 `system:` 是 Kubernetes 系统保留的，所以你要确保 所配置的用户名或者组名不能出现上述 `system:` 前缀。 除了对前缀的限制之外，RBAC 鉴权系统不对用户名格式作任何要求。

在 Kubernetes 中，鉴权模块提供用户组信息。 与用户名一样，用户组名也用字符串来表示，而且对该字符串没有格式要求， 只是不能使用保留的前缀 `system:`。

[服务账户](https://kubernetes.io/zh/docs/tasks/configure-pod-container/configure-service-account/) 的用户名前缀为 `system:serviceaccount:`，属于前缀为 `system:serviceaccounts:` 的用户组。

> **说明：**
>
> - `system:serviceaccount:` （单数）是用于服务账户用户名的前缀；
> - `system:serviceaccounts:` （复数）是用于服务账户组名的前缀。

#### RoleBinding 示例

下面示例是 `RoleBinding` 中的片段，仅展示其 `subjects` 的部分。

对于名称为 `alice@example.com` 的用户：

```yaml
subjects:
- kind: User
  name: "alice@example.com"
  apiGroup: rbac.authorization.k8s.io
```

对于名称为 `frontend-admins` 的用户组：

```yaml
subjects:
- kind: Group
  name: "frontend-admins"
  apiGroup: rbac.authorization.k8s.io
```

对于 `kube-system` 名字空间中的默认服务账户：

```yaml
subjects:
- kind: ServiceAccount
  name: default
  namespace: kube-system
```

对于任何名称空间中的 "qa" 组中所有的服务账户：

```yaml
subjects:
- kind: Group
  name: system:serviceaccounts:qa
  apiGroup: rbac.authorization.k8s.io
```

对于 "development" 名称空间中 "dev" 组中的所有服务帐户：

```yaml
subjects:
- kind: Group
  name: system:serviceaccounts:dev
  apiGroup: rbac.authorization.k8s.io
  namespace: development
```

对于在任何名字空间中的服务账户：

```yaml
subjects:
- kind: Group
  name: system:serviceaccounts
  apiGroup: rbac.authorization.k8s.io
```

对于所有已经过认证的用户：

```yaml
subjects:
- kind: Group
  name: system:authenticated
  apiGroup: rbac.authorization.k8s.io
```

对于所有未通过认证的用户：

```yaml
subjects:
- kind: Group
  name: system:unauthenticated
  apiGroup: rbac.authorization.k8s.io
```

对于所有用户：

```yaml
subjects:
- kind: Group
  name: system:authenticated
  apiGroup: rbac.authorization.k8s.io
- kind: Group
  name: system:unauthenticated
  apiGroup: rbac.authorization.k8s.io
```

## 默认 Roles 和 Role Bindings

API 服务器创建一组默认的 ClusterRole 和 ClusterRoleBinding 对象。 这其中许多是以 `system:` 为前缀的，用以标识对应资源是直接由集群控制面管理的。 所有的默认 ClusterRole 和 ClusterRoleBinding 都有 `kubernetes.io/bootstrapping=rbac-defaults` 标签。

> **注意：**
>
> 在修改名称包含 `system:` 前缀的 ClusterRole 和 ClusterRoleBinding 时要格外小心。 对这些资源的更改可能导致集群无法继续工作。

### 自动协商

在每次启动时，API 服务器都会更新默认 ClusterRole 以添加缺失的各种权限，并更新 默认的 ClusterRoleBinding 以增加缺失的的各类主体。 这种自动协商机制允许集群去修复一些不小心发生的修改，并且有助于保证角色和角色绑定 在新的发行版本中有权限或主体变更时仍然保持最新。

如果要禁止此功能，请将默认 ClusterRole 以及 ClusterRoleBinding 的 `rbac.authorization.kubernetes.io/autoupdate` 注解设置成 `false`。 注意，缺少默认权限和角色绑定主体可能会导致集群无法正常工作。

如果基于 RBAC 的鉴权机制被启用，则自动协商功能默认是被启用的。

### API 发现角色

无论是经过身份验证的还是未经过身份验证的用户，默认的角色绑定都授权他们读取被认为 是可安全地公开访问的 API（ 包括 CustomResourceDefinitions）。 如果要禁用匿名的未经过身份验证的用户访问，请在 API 服务器配置中中添加 `--anonymous-auth=false` 的配置选项。

通过运行命令 `kubectl` 可以查看这些角色的配置信息:

```shell
kubectl get clusterroles system:discovery -o yaml
```

> **说明：**
>
> 如果你编辑该 ClusterRole，你所作的变更会被 API 服务器在重启时自动覆盖，这是通过 [自动协商](https://kubernetes.io/zh/docs/reference/access-authn-authz/rbac/#auto-reconciliation)机制完成的。要避免这类覆盖操作， 要么不要手动编辑这些角色，要么禁止自动协商机制。

| 默认 ClusterRole              | 默认 ClusterRoleBinding                                   | 描述                                                         |
| ----------------------------- | --------------------------------------------------------- | ------------------------------------------------------------ |
| **system:basic-user**         | **system:authenticated** 组                               | 允许用户以只读的方式去访问他们自己的基本信息。在 1.14 版本之前，这个角色在默认情况下也绑定在 `system:unauthenticated` 上。 |
| **system:discovery**          | **system:authenticated** 组                               | 允许以只读方式访问 API 发现端点，这些端点用来发现和协商 API 级别。 在 1.14 版本之前，这个角色在默认情况下绑定在 `system:unauthenticated` 上。 |
| **system:public-info-viewer** | **system:authenticated** 和 **system:unauthenticated** 组 | 允许对集群的非敏感信息进行只读访问，它是在 1.14 版本中引入的。 |

### 面向用户的角色

一些默认的 ClusterRole 不是以前缀 `system:` 开头的。这些是面向用户的角色。 它们包括超级用户（Super-User）角色（`cluster-admin`）、 使用 ClusterRoleBinding 在集群范围内完成授权的角色（`cluster-status`）、 以及使用 RoleBinding 在特定名字空间中授予的角色（`admin`、`edit`、`view`）。

面向用户的 ClusterRole 使用 [ClusterRole 聚合](https://kubernetes.io/zh/docs/reference/access-authn-authz/rbac/#aggregated-clusterroles)以允许管理员在 这些 ClusterRole 上添加用于定制资源的规则。如果想要添加规则到 `admin`、`edit` 或者 `view`， 可以创建带有以下一个或多个标签的 ClusterRole：

```yaml
metadata:
  labels:
    rbac.authorization.k8s.io/aggregate-to-admin: "true"
    rbac.authorization.k8s.io/aggregate-to-edit: "true"
    rbac.authorization.k8s.io/aggregate-to-view: "true"
```

| 默认 ClusterRole  | 默认 ClusterRoleBinding | 描述                                                         |
| ----------------- | ----------------------- | ------------------------------------------------------------ |
| **cluster-admin** | **system:masters** 组   | 允许超级用户在平台上的任何资源上执行所有操作。 当在 **ClusterRoleBinding** 中使用时，可以授权对集群中以及所有名字空间中的全部资源进行完全控制。 当在 **RoleBinding** 中使用时，可以授权控制 RoleBinding 所在名字空间中的所有资源，包括名字空间本身。 |
| **admin**         | 无                      | 允许管理员访问权限，旨在使用 **RoleBinding** 在名字空间内执行授权。 如果在 **RoleBinding** 中使用，则可授予对名字空间中的大多数资源的读/写权限， 包括创建角色和角色绑定的能力。 但是它不允许对资源配额或者名字空间本身进行写操作。 |
| **edit**          | 无                      | 允许对名字空间的大多数对象进行读/写操作。 它不允许查看或者修改角色或者角色绑定。 不过，此角色可以访问 Secret，以名字空间中任何 ServiceAccount 的身份运行 Pods， 所以可以用来了解名字空间内所有服务账户的 API 访问级别。 |
| **view**          | 无                      | 允许对名字空间的大多数对象有只读权限。 它不允许查看角色或角色绑定。此角色不允许查看 Secrets，因为读取 Secret 的内容意味着可以访问名字空间中 ServiceAccount 的凭据信息，进而允许利用名字空间中任何 ServiceAccount 的 身份访问 API（这是一种特权提升）。 |

### 核心组件角色

| 默认 ClusterRole                   | 默认 ClusterRoleBinding                 | 描述                                                         |
| ---------------------------------- | --------------------------------------- | ------------------------------------------------------------ |
| **system:kube-scheduler**          | **system:kube-scheduler** 用户          | 允许访问 [scheduler](https://kubernetes.io/docs/reference/generated/kube-scheduler/) 组件所需要的资源。 |
| **system:volume-scheduler**        | **system:kube-scheduler** 用户          | 允许访问 kube-scheduler 组件所需要的卷资源。                 |
| **system:kube-controller-manager** | **system:kube-controller-manager** 用户 | 允许访问[控制器管理器](https://kubernetes.io/docs/reference/generated/kube-controller-manager/) 组件所需要的资源。 各个控制回路所需要的权限在[控制器角色](https://kubernetes.io/zh/docs/reference/access-authn-authz/rbac/#controller-roles) 详述。 |
| **system:node**                    | 无                                      | 允许访问 kubelet 所需要的资源，**包括对所有 Secret 的读操作和对所有 Pod 状态对象的写操作。**你应该使用 [Node 鉴权组件](https://kubernetes.io/zh/docs/reference/access-authn-authz/node/) 和 [NodeRestriction 准入插件](https://kubernetes.io/zh/docs/reference/access-authn-authz/admission-controllers/#noderestriction) 而不是 `system:node` 角色。同时基于 kubelet 上调度执行的 Pod 来授权 kubelet 对 API 的访问。`system:node` 角色的意义仅是为了与从 v1.8 之前版本升级而来的集群兼容。 |
| **system:node-proxier**            | **system:kube-proxy** 用户              | 允许访问 [kube-proxy](https://kubernetes.io/zh/docs/reference/command-line-tools-reference/kube-proxy/) 组件所需要的资源。 |

### 其他组件角色

| 默认 ClusterRole                         | 默认 ClusterRoleBinding                               | 描述                                                         |
| ---------------------------------------- | ----------------------------------------------------- | ------------------------------------------------------------ |
| **system:auth-delegator**                | 无                                                    | 允许将身份认证和鉴权检查操作外包出去。 这种角色通常用在插件式 API 服务器上，以实现统一的身份认证和鉴权。 |
| **system:heapster**                      | 无                                                    | 为 [Heapster](https://github.com/kubernetes/heapster) 组件（已弃用）定义的角色。 |
| **system:kube-aggregator**               | 无                                                    | 为 [kube-aggregator](https://github.com/kubernetes/kube-aggregator) 组件定义的角色。 |
| **system:kube-dns**                      | 在 **kube-system** 名字空间中的 **kube-dns** 服务账户 | 为 [kube-dns](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/) 组件定义的角色。 |
| **system:kubelet-api-admin**             | 无                                                    | 允许 kubelet API 的完全访问权限。                            |
| **system:node-bootstrapper**             | 无                                                    | 允许访问执行 [kubelet TLS 启动引导](https://kubernetes.io/docs/reference/command-line-tools-reference/kubelet-tls-bootstrapping/) 所需要的资源。 |
| **system:node-problem-detector**         | 无                                                    | 为 [node-problem-detector](https://github.com/kubernetes/node-problem-detector) 组件定义的角色。 |
| **system:persistent-volume-provisioner** | 无                                                    | 允许访问大部分 [动态卷驱动](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#provisioner) 所需要的资源。 |
| **system:monitoring**                    | **system:monitoring** 组                              | 允许对控制平面监控端点的读取访问（例如：[kube-apiserver](https://kubernetes.io/zh/docs/reference/command-line-tools-reference/kube-apiserver/) 存活和就绪端点（`/healthz`、`/livez`、`/readyz`）， 各个健康检查端点（`/healthz/*`、`/livez/*`、`/readyz/*`）和 `/metrics`）。 请注意，各个运行状况检查端点和度量标准端点可能会公开敏感信息。 |

### 内置控制器的角色

Kubernetes [控制器管理器](https://kubernetes.io/docs/reference/generated/kube-controller-manager/) 运行内建于 Kubernetes 控制面的[控制器](https://kubernetes.io/zh/docs/concepts/architecture/controller/)。 当使用 `--use-service-account-credentials` 参数启动时, kube-controller-manager 使用单独的服务账户来启动每个控制器。 每个内置控制器都有相应的、前缀为 `system:controller:` 的角色。 如果控制管理器启动时未设置 `--use-service-account-credentials`， 它使用自己的身份凭据来运行所有的控制器，该身份必须被授予所有相关的角色。 这些角色包括:

- `system:controller:attachdetach-controller`
- `system:controller:certificate-controller`
- `system:controller:clusterrole-aggregation-controller`
- `system:controller:cronjob-controller`
- `system:controller:daemon-set-controller`
- `system:controller:deployment-controller`
- `system:controller:disruption-controller`
- `system:controller:endpoint-controller`
- `system:controller:expand-controller`
- `system:controller:generic-garbage-collector`
- `system:controller:horizontal-pod-autoscaler`
- `system:controller:job-controller`
- `system:controller:namespace-controller`
- `system:controller:node-controller`
- `system:controller:persistent-volume-binder`
- `system:controller:pod-garbage-collector`
- `system:controller:pv-protection-controller`
- `system:controller:pvc-protection-controller`
- `system:controller:replicaset-controller`
- `system:controller:replication-controller`
- `system:controller:resourcequota-controller`
- `system:controller:root-ca-cert-publisher`
- `system:controller:route-controller`
- `system:controller:service-account-controller`
- `system:controller:service-controller`
- `system:controller:statefulset-controller`
- `system:controller:ttl-controller`

## 初始化与预防权限提升

RBAC API 会阻止用户通过编辑角色或者角色绑定来提升权限。 由于这一点是在 API 级别实现的，所以在 RBAC 鉴权组件未启用的状态下依然可以正常工作。

### 对角色创建或更新的限制

只有在符合下列条件之一的情况下，你才能创建/更新角色:

1. 你已经拥有角色中包含的所有权限，且其作用域与正被修改的对象作用域相同。 （对 ClusterRole 而言意味着集群范围，对 Role 而言意味着相同名字空间或者集群范围）。
2. 你被显式授权在 `rbac.authorization.k8s.io` API 组中的 `roles` 或 `clusterroles` 资源 使用 `escalate` 动词。

例如，如果 `user-1` 没有列举集群范围所有 Secret 的权限，他将不能创建包含该权限的 ClusterRole。 若要允许用户创建/更新角色：

1. 根据需要赋予他们一个角色，允许他们根据需要创建/更新 Role 或者 ClusterRole 对象。
2. 授予他们在所创建/更新角色中包含特殊权限的权限:
   - 隐式地为他们授权（如果它们试图创建或者更改 Role 或 ClusterRole 的权限， 但自身没有被授予相应权限，API 请求将被禁止）。
   - 通过允许他们在 Role 或 ClusterRole 资源上执行 `escalate` 动作显式完成授权。 这里的 `roles` 和 `clusterroles` 资源包含在 `rbac.authorization.k8s.io` API 组中。

### 对角色绑定创建或更新的限制

只有你已经具有了所引用的角色中包含的全部权限时，或者你被授权在所引用的角色上执行 `bind` 动词时，你才可以创建或更新角色绑定。这里的权限与角色绑定的作用域相同。 例如，如果用户 `user-1` 没有列举集群范围所有 Secret 的能力，则他不可以创建 ClusterRoleBinding 引用授予该许可权限的角色。 如要允许用户创建或更新角色绑定：

1. 赋予他们一个角色，使得他们能够根据需要创建或更新 RoleBinding 或 ClusterRoleBinding 对象。
2. 授予他们绑定某特定角色所需要的许可权限：
   - 隐式授权下，可以将角色中包含的许可权限授予他们；
   - 显式授权下，可以授权他们在特定 Role （或 ClusterRole）上执行 `bind` 动词的权限。

例如，下面的 ClusterRole 和 RoleBinding 将允许用户 `user-1` 把名字空间 `user-1-namespace` 中的 `admin`、`edit` 和 `view` 角色赋予其他用户：

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: role-grantor
rules:
- apiGroups: ["rbac.authorization.k8s.io"]
  resources: ["rolebindings"]
  verbs: ["create"]
- apiGroups: ["rbac.authorization.k8s.io"]
  resources: ["clusterroles"]
  verbs: ["bind"]
  # 忽略 resourceNames 意味着允许绑定任何 ClusterRole
  resourceNames: ["admin","edit","view"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: role-grantor-binding
  namespace: user-1-namespace
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: role-grantor
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: User
  name: user-1
```

当启动引导第一个角色和角色绑定时，需要为初始用户授予他们尚未拥有的权限。 对初始角色和角色绑定进行初始化时需要：

- 使用用户组为 `system:masters` 的凭据，该用户组由默认绑定关联到 `cluster-admin` 这个超级用户角色。
- 如果你的 API 服务器启动时启用了不安全端口（使用 `--insecure-port`）, 你也可以通过 该端口调用 API ，这样的操作会绕过身份验证或鉴权。

## 一些命令行工具

### `kubectl create role`

创建 Role 对象，定义在某一名字空间中的权限。例如:

- 创建名称为 "pod-reader" 的 Role 对象，允许用户对 Pods 执行 `get`、`watch` 和 `list` 操作：

  ```shell
  kubectl create role pod-reader --verb=get --verb=list --verb=watch --resource=pods
  ```

- 创建名称为 "pod-reader" 的 Role 对象并指定 `resourceNames`：

  ```shell
  kubectl create role pod-reader --verb=get --resource=pods --resource-name=readablepod --resource-name=anotherpod
  ```

- 创建名为 "foo" 的 Role 对象并指定 `apiGroups`：

  ```shell
  kubectl create role foo --verb=get,list,watch --resource=replicasets.apps
  ```

- 创建名为 "foo" 的 Role 对象并指定子资源权限:

  ```shell
  kubectl create role foo --verb=get,list,watch --resource=pods,pods/status
  ```

- 创建名为 "my-component-lease-holder" 的 Role 对象，使其具有对特定名称的 资源执行 get/update 的权限：

  ```shell
  kubectl create role my-component-lease-holder --verb=get,list,watch,update --resource=lease --resource-name=my-component
  ```

### `kubectl create clusterrole`

创建 ClusterRole 对象。例如：

- 创建名称为 "pod-reader" 的 ClusterRole`对象，允许用户对 Pods 对象执行 `get`、`watch`和`list` 操作：

  ```shell
  kubectl create clusterrole pod-reader --verb=get,list,watch --resource=pods
  ```

- 创建名为 "pod-reader" 的 ClusterRole 对象并指定 `resourceNames`：

  ```shell
  kubectl create clusterrole pod-reader --verb=get --resource=pods --resource-name=readablepod --resource-name=anotherpod
  ```

- 创建名为 "foo" 的 ClusterRole 对象并指定 `apiGroups`：

  ```shell
  kubectl create clusterrole foo --verb=get,list,watch --resource=replicasets.apps
  ```

- 创建名为 "foo" 的 ClusterRole 对象并指定子资源:

  ```shell
  kubectl create clusterrole foo --verb=get,list,watch --resource=pods,pods/status
  ```

- 创建名为 "foo" 的 ClusterRole 对象并指定 `nonResourceURL`：

  ```shell
  kubectl create clusterrole "foo" --verb=get --non-resource-url=/logs/*
  ```

- 创建名为 "monitoring" 的 ClusterRole 对象并指定 `aggregationRule`：

  ```shell
  kubectl create clusterrole monitoring --aggregation-rule="rbac.example.com/aggregate-to-monitoring=true"
  ```

### `kubectl create rolebinding`

在特定的名字空间中对 `Role` 或 `ClusterRole` 授权。例如：

- 在名字空间 "acme" 中，将名为 `admin` 的 ClusterRole 中的权限授予名称 "bob" 的用户:

  ```shell
  kubectl create rolebinding bob-admin-binding --clusterrole=admin --user=bob --namespace=acme
  ```

- 在名字空间 "acme" 中，将名为 `view` 的 ClusterRole 中的权限授予名字空间 "acme" 中名为 `myapp` 的服务账户：

  ```shell
  kubectl create rolebinding myapp-view-binding --clusterrole=view --serviceaccount=acme:myapp --namespace=acme
  ```

- 在名字空间 "acme" 中，将名为 `view` 的 ClusterRole 对象中的权限授予名字空间 "myappnamespace" 中名称为 `myapp` 的服务账户：

  ```shell
  kubectl create rolebinding myappnamespace-myapp-view-binding --clusterrole=view --serviceaccount=myappnamespace:myapp --namespace=acme
  ```

### `kubectl create clusterrolebinding`

在整个集群（所有名字空间）中用 ClusterRole 授权。例如：

- 在整个集群范围，将名为 `cluster-admin` 的 ClusterRole 中定义的权限授予名为 "root" 用户：

  ```shell
  kubectl create clusterrolebinding root-cluster-admin-binding --clusterrole=cluster-admin --user=root
  ```

- 在整个集群范围内，将名为 `system:node-proxier` 的 ClusterRole 的权限授予名为 "system:kube-proxy" 的用户：

  ```shell
  kubectl create clusterrolebinding kube-proxy-binding --clusterrole=system:node-proxier --user=system:kube-proxy
  ```

- 在整个集群范围内，将名为 `view` 的 ClusterRole 中定义的权限授予 "acme" 名字空间中 名为 "myapp" 的服务账户：

  ```shell
  kubectl create clusterrolebinding myapp-view-binding --clusterrole=view --serviceaccount=acme:myapp
  ```

### `kubectl auth reconcile`

使用清单文件来创建或者更新 `rbac.authorization.k8s.io/v1` API 对象。

尚不存在的对象会被创建，如果对应的名字空间也不存在，必要的话也会被创建。 已经存在的角色会被更新，使之包含输入对象中所给的权限。如果指定了 `--remove-extra-permissions`，可以删除额外的权限。

已经存在的绑定也会被更新，使之包含输入对象中所给的主体。如果指定了 `--remove-extra-permissions`，则可以删除多余的主体。

例如:

- 测试应用 RBAC 对象的清单文件，显示将要进行的更改：

  ```shell
  kubectl auth reconcile -f my-rbac-rules.yaml --dry-run
  ```

- 应用 RBAC 对象的清单文件，保留角色中的额外权限和绑定中的其他主体：

  ```shell
  kubectl auth reconcile -f my-rbac-rules.yaml
  ```

- 应用 RBAC 对象的清单文件, 删除角色中的额外权限和绑定中的其他主体：

  ```shell
  kubectl auth reconcile -f my-rbac-rules.yaml --remove-extra-subjects --remove-extra-permissions
  ```

查看 CLI 帮助获取详细的用法。

## 服务账户权限

默认的 RBAC 策略为控制面组件、节点和控制器授予权限。 但是不会对 `kube-system` 名字空间之外的服务账户授予权限。 （除了授予所有已认证用户的发现权限）

这使得你可以根据需要向特定服务账户授予特定权限。 细粒度的角色绑定可带来更好的安全性，但需要更多精力管理。 粗粒度的授权可能导致服务账户被授予不必要的 API 访问权限（甚至导致潜在的权限提升）， 但更易于管理。

按从最安全到最不安全的顺序，存在以下方法：

1. 为特定应用的服务账户授予角色（最佳实践）

   这要求应用在其 Pod 规约中指定 `serviceAccountName`， 并额外创建服务账户（包括通过 API、应用程序清单、`kubectl create serviceaccount` 等）。

   例如，在名字空间 "my-namespace" 中授予服务账户 "my-sa" 只读权限：

   ```shell
   kubectl create rolebinding my-sa-view \
     --clusterrole=view \
     --serviceaccount=my-namespace:my-sa \
     --namespace=my-namespace
   ```

1. 将角色授予某名字空间中的 "default" 服务账户

   如果某应用没有指定 `serviceAccountName`，那么它将使用 "default" 服务账户。

   > **说明：** "default" 服务账户所具有的权限会被授予给名字空间中所有未指定 `serviceAccountName` 的 Pod。

   例如，在名字空间 "my-namespace" 中授予服务账户 "default" 只读权限：

   ```shell
   kubectl create rolebinding default-view \
     --clusterrole=view \
     --serviceaccount=my-namespace:default \
     --namespace=my-namespace
   ```

   许多[插件组件](https://kubernetes.io/zh/docs/concepts/cluster-administration/addons/) 在 `kube-system` 名字空间以 "default" 服务账户运行。 要允许这些插件组件以超级用户权限运行，需要将集群的 `cluster-admin` 权限授予 `kube-system` 名字空间中的 "default" 服务账户。

   > **说明：** 启用这一配置意味着在 `kube-system` 名字空间中包含以超级用户账号来访问 API 的 Secrets。

   ```shell
   kubectl create clusterrolebinding add-on-cluster-admin \
     --clusterrole=cluster-admin \
     --serviceaccount=kube-system:default
   ```

1. 将角色授予名字空间中所有服务账户

   如果你想要名字空间中所有应用都具有某角色，无论它们使用的什么服务账户， 可以将角色授予该名字空间的服务账户组。

   例如，在名字空间 "my-namespace" 中的只读权限授予该名字空间中的所有服务账户：

   ```shell
   kubectl create rolebinding serviceaccounts-view \
     --clusterrole=view \
     --group=system:serviceaccounts:my-namespace \
     --namespace=my-namespace
   ```

1. 在集群范围内为所有服务账户授予一个受限角色（不鼓励）

   如果你不想管理每一个名字空间的权限，你可以向所有的服务账户授予集群范围的角色。

   例如，为集群范围的所有服务账户授予跨所有名字空间的只读权限：

   ```shell
   kubectl create clusterrolebinding serviceaccounts-view \
     --clusterrole=view \
     --group=system:serviceaccounts
   ```

1. 授予超级用户访问权限给集群范围内的所有服务帐户（强烈不鼓励）

   如果你不关心如何区分权限，你可以将超级用户访问权限授予所有服务账户。

   > **警告：** 这样做会允许所有应用都对你的集群拥有完全的访问权限，并将允许所有能够读取 Secret（或创建 Pod）的用户对你的集群有完全的访问权限。

   ```shell
   kubectl create clusterrolebinding serviceaccounts-cluster-admin \
     --clusterrole=cluster-admin \
     --group=system:serviceaccounts
   ```

## 从 ABAC 升级

原来运行较老版本 Kubernetes 的集群通常会使用限制宽松的 ABAC 策略， 包括授予所有服务帐户全权访问 API 的能力。

默认的 RBAC 策略为控制面组件、节点和控制器等授予有限的权限，但不会为 `kube-system` 名字空间外的服务账户授权 （除了授予所有认证用户的发现权限之外）。

这样做虽然安全得多，但可能会干扰期望自动获得 API 权限的现有工作负载。 这里有两种方法来完成这种转换:

### 并行鉴权

同时运行 RBAC 和 ABAC 鉴权模式, 并指定包含 [现有的 ABAC 策略](https://kubernetes.io/zh/docs/reference/access-authn-authz/abac/#policy-file-format) 的策略文件：

```shell
--authorization-mode=RBAC,ABAC --authorization-policy-file=mypolicy.json
```

关于命令行中的第一个选项：如果早期的鉴权组件，例如 Node，拒绝了某个请求，则 RBAC 鉴权组件尝试对该 API 请求鉴权。如果 RBAC 也拒绝了该 API 请求，则运行 ABAC 鉴权组件。这意味着被 RBAC 或 ABAC 策略所允许的任何请求都是被允许的请求。

如果 API 服务器启动时，RBAC 组件的日志级别为 5 或更高（`--vmodule=rbac*=5` 或 `--v=5`）， 你可以在 API 服务器的日志中看到 RBAC 的细节 （前缀 `RBAC:`） 你可以使用这些信息来确定需要将哪些角色授予哪些用户、组或服务帐户。

一旦你[将角色授予服务账户](https://kubernetes.io/zh/docs/reference/access-authn-authz/rbac/#service-account-permissions) ，工作负载运行时 在服务器日志中没有出现 RBAC 拒绝消息，就可以删除 ABAC 鉴权器。

## 宽松的 RBAC 权限

你可以使用 RBAC 角色绑定在多个场合使用宽松的策略。

> **警告：**
>
> 下面的策略允许 **所有** 服务帐户充当集群管理员。 容器中运行的所有应用程序都会自动收到服务帐户的凭据，可以对 API 执行任何操作， 包括查看 Secrets 和修改权限。这一策略是不被推荐的。
>
> ```shell
> kubectl create clusterrolebinding permissive-binding \
>   --clusterrole=cluster-admin \
>   --user=admin \
>   --user=kubelet \
>   --group=system:serviceaccounts
> ```

在你完成到 RBAC 的迁移后，应该调整集群的访问控制，确保相关的策略满足你的 信息安全需求。
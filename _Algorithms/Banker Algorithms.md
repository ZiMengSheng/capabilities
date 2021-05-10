# 银行家算法

[**银行家算法**](https://baike.baidu.com/item/银行家算法)（Banker's Algorithm）是一个避免死锁（Deadlock）的著名算法，是由艾兹格·迪杰斯特拉在1965年为T.H.E系统设计的一种避免死锁产生的算法。它以银行借贷系统的分配策略为基础，判断并保证系统的安全运行。 [1] 

## 背景简介

在银行中，客户申请贷款的数量是有限的，每个客户在第一次申请贷款时要声明完成该项目所需的最大资金量，在满足所有贷款要求时，客户应及时归还。银行家在客户申请的贷款数量不超过自己拥有的最大值时，都应尽量满足客户的需要。在这样的描述中，银行家就好比操作系统，资金就是资源，客户就相当于要申请资源的进程。

银行家算法是一种最有代表性的避免[死锁](https://baike.baidu.com/item/死锁)的算法。在避免[死锁](https://baike.baidu.com/item/死锁)方法中允许进程动态地申请资源，但系

[![银行家算法](OS%20DeadLock.assets/1c950a7b02087bf4d6f425e3f2d3572c11dfcfa4)](https://baike.baidu.com/pic/银行家算法/1679781/0/b3ba5d16b107087020a4e9c5?fr=lemma&ct=single)银行家算法

统在进行资源分配之前，应先计算此次分配资源的安全性，若分配不会导致系统进入不安全状态，则分配，否则等待。为实现银行家算法，系统必须设置若干[数据结构](https://baike.baidu.com/item/数据结构)。

要解释银行家算法，必须先解释[操作系统](https://baike.baidu.com/item/操作系统)[安全状态](https://baike.baidu.com/item/安全状态)和不安全状态。

安全序列是指一个进程序列{P1，…，Pn}是安全的，即对于每一个进程Pi(1≤i≤n），它以后尚需要的资源量不超过系统当前剩余资源量与所有进程Pj (j < i )当前占有资源量之和。

## 安全状态

如果存在一个由系统中所有进程构成的安全序列P1，…，Pn，则系统处于安全状态。安全状态一定是没有[死锁](https://baike.baidu.com/item/死锁)发生。

## 不安全状态

不存在一个安全序列。不安全状态不一定导致[死锁](https://baike.baidu.com/item/死锁)。

## 数据结构

1）可利用资源向量Available

是个含有m个元素的[数组](https://baike.baidu.com/item/数组)，其中的每一个元素代表一类可利用的资源数目。如果Available[j]=K，则表示系统中现有Rj类资源K个。

2）最大需求[矩阵](https://baike.baidu.com/item/矩阵)Max

这是一个n×m的[矩阵](https://baike.baidu.com/item/矩阵)，它定义了系统中n个进程中的每一个进程对m类资源的最大需求。如果Max[i,j]=K，则表示进程i需要Rj类资源的最大数目为K。

3）分配[矩阵](https://baike.baidu.com/item/矩阵)Allocation

这也是一个n×m的[矩阵](https://baike.baidu.com/item/矩阵)，它定义了系统中每一类资源当前已分配给每一进程的资源数。如果Allocation[i,j]=K，则表示进程i当前已分得Rj类资源的 数目为K。

4）需求[矩阵](https://baike.baidu.com/item/矩阵)Need。

这也是一个n×m的[矩阵](https://baike.baidu.com/item/矩阵)，用以表示每一个进程尚需的各类资源数。如果Need[i,j]=K，则表示进程i还需要Rj类资源K个，方能完成其任务。

Need[i,j]=Max[i,j]-Allocation[i,j]

## 算法原理

我们可以把[操作系统](https://baike.baidu.com/item/操作系统)看作是银行家，操作系统管理的资源相当于银行家管理的资金，进程向操作系统请求分配资源相当于用户向银行家贷款。

为保证资金的安全，银行家规定：

(1) 当一个顾客对资金的最大需求量不超过银行家现有的资金时就可接纳该顾客；

(2) 顾客可以分期贷款，但贷款的总数不能超过最大需求量；

(3) 当银行家现有的资金不能满足顾客尚需的贷款数额时，对顾客的贷款可推迟支付，但总能使顾客在有限的时间里得到贷款；

(4) 当顾客得到所需的全部资金后，一定能在有限的时间里归还所有的资金.

[操作系统](https://baike.baidu.com/item/操作系统)按照银行家制定的规则为进程分配资源，当进程首次申请资源时，要测试该进程对资源的最大需求量，如果系统现存的资源可以满足它的最大需求量则按当前的申请量分配资源，否则就推迟分配。当进程在执行中继续申请资源时，先测试该进程本次申请的资源数是否超过了该资源所剩余的总量。若超过则拒绝分配资源，若能满足则按当前的申请量分配资源，否则也要推迟分配。

## 算法实现

### 初始化

由用户输入数据，分别对可利用资源向量[矩阵](https://baike.baidu.com/item/矩阵)AVAILABLE、最大需求矩阵MAX、分配矩阵ALLOCATION、需求矩阵NEED赋值。

### 银行家算法

在避免[死锁](https://baike.baidu.com/item/死锁)的方法中，所施加的限制条件较弱，有可能获得令人满意的系统性能。在该方法中把系统的状态分为安全状态和不安全状态，只要能使系统始终都处于安全状态，便可以避免发生[死锁](https://baike.baidu.com/item/死锁)。

银行家算法的基本思想是分配资源之前，判断系统是否是安全的；若是，才分配。它是最具有代表性的避免[死锁](https://baike.baidu.com/item/死锁)的算法。

设进程cusneed提出请求REQUEST [i]，则银行家算法按如下规则进行判断。

(1)如果REQUEST [cusneed] [i]<= NEED\[cusneed][i]，则转(2)；否则，出错。

(2)如果REQUEST [cusneed] [i]<= AVAILABLE[i]，则转(3)；否则，等待。

(3)系统试探分配资源，修改相关数据：

AVAILABLE[i]-=REQUEST\[cusneed][i];

ALLOCATION\[cusneed][i]+=REQUEST\[cusneed][i];

NEED\[cusneed][i]-=REQUEST\[cusneed][i];

(4)系统执行安全性检查，如安全，则分配成立；否则试探险性分配作废，系统恢复原状，进程等待。

### 安全性检查算法

(1)设置两个工作向量Work=AVAILABLE;FINISH

(2)从进程集合中找到一个满足下述条件的进程，

FINISH==false;

NEED<=Work;

如找到，执行（3)；否则，执行（4)

(3)设进程获得资源，可顺利执行，直至完成，从而释放资源。

Work=Work+ALLOCATION;

Finish=true;

GOTO 2

(4)如所有的进程Finish= true，则表示安全；否则系统不安全。
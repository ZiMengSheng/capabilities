# Design Pattern

In software engineering, a design pattern is a general repeatable solution to a commonly occurring problem in software design.

# Creational Pattern

创建型模式的主要关注点是“怎样创建对象？”，它的主要特点是“将对象的创建与使用分离”。这样可以降低系统的耦合度，使用者不需要关注对象的创建细节，对象的创建由相关的工厂来完成。就像我们去商场购买商品时，不需要知道商品是怎么生产出来一样，因为它们由专门的厂商生产。

## Singleton

![单例模式的结构图](.\design pattern pic\singleton.gif)

### Coding

#### 懒汉

```java
public class Singleton {
	private static Singleton instance = null;

	private Singleton() {
	 
	}

	public static Singleton getInstance() {
		if (instance == null) {
			instance = new Singleton();
		}
		return instance;
	}
}

// 线程安全
public class Singleton {
	private volatile static Singleton instance = null;
 
	private Singleton() {
 
	}
 
	public static Singleton getInstance() {
		if (instance == null) {
			synchronized (Singleton.class) {
				if (instance == null) {
					instance = new Singleton();
				}
			}
		}
		return instance;
	}
}
```

#### 饿汉

```java
public class Singleton {
	private static Singleton instance = new Singleton();

	private Singleton() {
	 
	}
	 
	public static Singleton getInstance() {
		return instance;
	}
}
```

#### 枚举

```java
public enum Singleton {      
	INSTANCE;      
	public void whateverMethod() 
	{      
	}   
}
```

#### 内部类

```java

public class Singleton {      
	private static class SingletonHolder {      
		private static final Singleton INSTANCE = new Singleton();      
	}      
	private Singleton (){}      
	public static final Singleton getInstance() {      
		return SingletonHolder.INSTANCE;      
	}   
}
```

## Abstract Factory

![抽象工厂模式的结构图](.\design pattern pic\Abstract Factory.gif)

## Builder

![建造者模式的结构图](.\design pattern pic\Builder.gif)\

# Structural Pattern

## Proxy

![Proxy scheme](.\design pattern pic\Proxy.png)

### Dynamic Proxy

![动态代理模式的结构图](.\design pattern pic\Dynamic Proxy.gif)

## Decorator

![装饰模式的结构图](.\design pattern pic\decorator.gif)

## Adapter

### 类适配器

![类适配器模式的结构图](.\design pattern pic\class adapter.gif)

### 对象适配器

![对象适配器模式的结构图](.\design pattern pic\object adapter.gif)

## Bridge

![桥接模式的结构图](.\design pattern pic\Bridge.gif)



## Facade

![外观模式的结构图](.\design pattern pic\Facade.gif)

## Composite

### 透明组合模式

![透明式的组合模式的结构图](.\design pattern pic\transparent composite.gif)

### 安全

![安全式的组合模式的结构图](.\design pattern pic\secure composite.gif)

## Flyweight

![享元模式的结构图](.\design pattern pic\Flyweight.gif)

### coding

```csharp
public abstract class Chess {
    protected int x;
    protected int y;
    public abstract void play(int x,int y);
}
```

```java
public class WhiteChess extends Chess {
    @Override
    public void play(int x, int y) {
        this.x = x;
        this.y = y;
        System.out.println("○:(" + this.x + "," + this.y + ")");
    }
}
```

```java
public class BlackChess extends Chess {
    @Override
    public void play(int x, int y) {
        this.x = x;
        this.y = y;
        System.out.println("●:(" + this.x + "," + this.y + ")");
    }
}
```

```csharp
public class ChessFactory {
    private static Chess whiteChess;
    private static Chess blackChess;
    public static Chess getChess(int key) {
        if (key == 0) {
            if (whiteChess == null) {
                System.out.println("=====创建白棋对象======");
                whiteChess = new WhiteChess();
            }
            return whiteChess;
        } else {
            if (blackChess == null) {
                System.out.println("=====创建黑棋对象======");
                blackChess = new BlackChess();
            }
            return blackChess;
        }
    }
}
```

# Behavioral Pattern

## Template Method

![模板方法模式的结构图](.\design pattern pic\Template Method.gif)

## Chain of Responsibility

![责任链模式的结构图](.\design pattern pic\Chain of Responsibility.gif)

## Command

![命令模式的结构图](.\design pattern pic\Command.gif)



## Observer

![观察者模式的结构图](.\design pattern pic\Observer.gif)

## Mediator

![中介者模式的结构图](.\design pattern pic\Mediator.gif)



## Visitor

![访问者（Visitor）模式的结构图](.\design pattern pic\Visitor.gif)

## Iterator

![Iterator example](.\design pattern pic\Iterator.png)

## State

![状态模式的结构图](.\design pattern pic\State.gif)

## Strategy

![策略模式的结构图](.\design pattern pic\Strategy.gif)

## Interpreter

![Scheme of Interpreter](.\design pattern pic\Interpreter.png)

## Memento

![备忘录模式的结构图](.\design pattern pic\Memento.gif)
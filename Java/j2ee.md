# IOC

In the Java community there's been a rush of lightweight containers that help to assemble components from different projects into a cohesive application. Underlying these containers is a common pattern to how they perform the wiring, a concept they refer under the very generic name of "Inversion of Control".

In this article I dig into how this pattern works, under the more specic name of "Dependency Injection", and contrast it with the Service Locator alternative. The choice between them is less important than the principle of separating conguration from use.

## Component and Service

I use component to mean a glob of software that's intended to be used, without change, by an application that is out of the control of the writers of the component. By 'without change' I mean that the using application doesn't change the source code of the components, although they may alter the component's behavior by extending it in ways allowed by the component writers. 

A service is similar to a component in that it's used by foreign applications. The main difference is that I expect a component to be used locally (think jar le, assembly, dll, or a source import). A service will be used remotely through some remote interface, either synchronous or asynchronous (eg web service, messaging system, RPC, or socket.)

## Example

```java
class MovieLister... 
    private MovieFinder finder;
	
	public MovieLister() {
		finder = new ColonDelimitedMovieFinder("movies1.txt");
	}

    public Movie[] moviesDirectedBy(String arg) { 
    	List allMovies = finder.findAll(); 
    	for (Iterator it = allMovies.iterator(); it.hasNext();) 
        { 
            Movie movie = (Movie) it.next(); 
            if (!movie.getDirector().equals(arg)) 
                it.remove(); 
        } 
    	return (Movie[]) allMovies.toArray(new Movie[allMovies.size()]); 
	}
```

```java
public interface MovieFinder { 
    List findAll(); 
}
```

![image-20201203161845531](.\j2ee pic\example.png)



## Dependency Injection

The basic idea of the Dependency Injection is to have a separate object, an assembler, that populates a field in the lister class with an appropriate implementation for the finder interface.

![img](.\j2ee pic\Dependency Injection.gif)

### Constructor Injection

```java
class MovieLister...
	public MovieLister(MovieFinder finder) {
    	this.finder = finder;       
  	}
```

```java
class ColonMovieFinder...
  	public ColonMovieFinder(String filename) {
      	this.filename = filename;
  	}
```

```java
private MutablePicoContainer configureContainer() {
    MutablePicoContainer pico = new DefaultPicoContainer();
    Parameter[] finderParams =  {new ConstantParameter("movies1.txt")};
    pico.registerComponentImplementation(MovieFinder.class, ColonMovieFinder.class, finderParams);
    pico.registerComponentImplementation(MovieLister.class);
    return pico;
}

public void testWithPico() {
    MutablePicoContainer pico = configureContainer();
    MovieLister lister = (MovieLister) pico.getComponentInstance(MovieLister.class);
    Movie[] movies = lister.moviesDirectedBy("Sergio Leone");
    assertEquals("Once Upon a Time in the West", movies[0].getTitle());
}
```

### Setter Injection

```java
class MovieLister...
	private MovieFinder finder;
	public void setFinder(MovieFinder finder) {
  		this.finder = finder;
	}
```

```java
class ColonMovieFinder...
  public void setFilename(String filename) {
      this.filename = filename;
  }
```

```xml
<beans>
    <bean id="MovieLister" class="spring.MovieLister">
        <property name="finder">
            <ref local="MovieFinder"/>
        </property>
    </bean>
    <bean id="MovieFinder" class="spring.ColonMovieFinder">
        <property name="filename">
            <value>movies1.txt</value>
        </property>
    </bean>
</beans>
```

```java
public void testWithSpring() throws Exception {
    ApplicationContext ctx = new FileSystemXmlApplicationContext("spring.xml");
    MovieLister lister = (MovieLister) ctx.getBean("MovieLister");
    Movie[] movies = lister.moviesDirectedBy("Sergio Leone");
    assertEquals("Once Upon a Time in the West", movies[0].getTitle());
}
```

### Interface Injection

```java
public interface InjectFinder {
    void injectFinder(MovieFinder finder);
}
```

```java
class MovieLister implements InjectFinder  
  	public void injectFinder(MovieFinder finder) {
      	this.finder = finder;
  	}
```

```java
public interface InjectFinderFilename {
    void injectFilename (String filename);
}
```

```java
class ColonMovieFinder implements MovieFinder, InjectFinderFilename...
  	public void injectFilename(String filename) {
      	this.filename = filename;
  	}

 	public void inject(Object target) {
    	((InjectFinder) target).injectFinder(this);        
  	}
```

```java
class Tester...
  	private Container container;

   	private void configureContainer() {
     	container = new Container();
     	registerComponents();
     	registerInjectors();
     	container.start();
  	}

	private void registerComponents() {
    	container.registerComponent("MovieLister", MovieLister.class);
    	container.registerComponent("MovieFinder", ColonMovieFinder.class);
  	}
  	private void registerInjectors() {
    	container.registerInjector(InjectFinder.class, container.lookup("MovieFinder"));
    	container.registerInjector(InjectFinderFilename.class, new FinderFilenameInjector());
  	}

	public interface Injector {
  		public void inject(Object target);
	}

	public static class FinderFilenameInjector implements Injector {
    	public void inject(Object target) {
      		((InjectFinderFilename)target).injectFilename("movies1.txt");      
    	}
  	}

  	public void testIface() {
    	configureContainer();
    	MovieLister lister = (MovieLister)container.lookup("MovieLister");
    	Movie[] movies = lister.moviesDirectedBy("Sergio Leone");
    	assertEquals("Once Upon a Time in the West", movies[0].getTitle());
  	}
```

## Service Locator

The basic idea behind a service locator is to have an object that knows how to get hold of all of the services that an application might need. So a service locator for this application would have a method that returns a movie finder when one is needed. Of course this just shifts the burden a tad, we still have to get the locator into the lister.

![img](.\j2ee pic\service locator.gif)

.

```java
class MovieLister..
  	MovieFinder finder = ServiceLocator.movieFinder();
```

```java
class ServiceLocator...
    public static MovieFinder movieFinder() {
        return soleInstance.movieFinder;
    }
    private static ServiceLocator soleInstance;
    private MovieFinder movieFinder;

	public static void load(ServiceLocator arg) {
      	soleInstance = arg;
  	}

  	public ServiceLocator(MovieFinder movieFinder) {
      	this.movieFinder = movieFinder;
  	}
```

```java
class Tester...
  	private void configure() {
      ServiceLocator.load(new ServiceLocator(new ColonMovieFinder("movies1.txt")));
  	}
    public void testSimple() {
      	configure();
      	MovieLister lister = new MovieLister();
      	Movie[] movies = lister.moviesDirectedBy("Sergio Leone");
      	assertEquals("Once Upon a Time in the West", movies[0].getTitle());
  	}
```

### A Dynamic Service Locator

```java
class ServiceLocator...
  private static ServiceLocator soleInstance;
  public static void load(ServiceLocator arg) {
      soleInstance = arg;
  }
  private Map services = new HashMap();
  public static Object getService(String key){
      return soleInstance.services.get(key);
  }
  public void loadService (String key, Object service) {
      services.put(key, service);
  }
```

```java
class Tester...
  private void configure() {
      ServiceLocator locator = new ServiceLocator();
      locator.loadService("MovieFinder", new ColonMovieFinder("movies1.txt"));
      ServiceLocator.load(locator);
  }
```

```java
class MovieLister...
  MovieFinder finder = (MovieFinder) ServiceLocator.getService("MovieFinder");
```

## Deciding which option to use

### Service Locator vs Dependency Injection

### Constructor versus Setter Injection

### Code or configuration files

### Separating Configuration from Use
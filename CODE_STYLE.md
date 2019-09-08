Code Style
----------

This document describes the adopted standards for the code style:

 * Classes that implement the utility classes layer between Models and Controllers should be suffixed with 'Repo' (as in Repository Pattern)
 
 
Class Structuring
----------------- 

Classes structure is as follows:

```java

class {
    companion object {
    }
    
    // Class variables go here
    
    // Overriden (inherited) methods go here
    
    // Public (non event-related) class methods go here
    
    // Private class methods go here
    
    // Public event-related methods go here
}

```

View naming
-----------

Views that are declared under XML should start with an acronym that represents the view type, so to
make it easy to identify the view type in code. Example, a _TextView_ would be called __tv__\_name .


Package Structure rules
------------------------

 * *helper* : This package will contain helper classes that abstract some behavior for a specific component, but that need to reference the class that contains them or some object inside that class.
 * *manager* : Globally accessible singletons that are used throughout the app to control state.
 * *repo* : Repository classes.
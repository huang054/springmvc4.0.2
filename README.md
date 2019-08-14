# springmvc4.0.2
https://www.cnblogs.com/fangjian0423/p/springMVC-directory-summary.html
#springioc，aop
https://www.cnblogs.com/xrq730/p/6285358.html

http://www.imooc.com/article/34150
关于BeanPostProcessor中各个回调调用的顺序
1、InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation(beanClass, beanName)
    该方法在创建对象之前会先掉用，如果有返回实例则直接使用不会去走下面创建对象的逻辑，并在之后执行
        BeanPostProcessor.postProcessAfterInitialization(result, beanName)
2、SmartInstantiationAwareBeanPostProcessor.determineCandidateConstructors(beanClass, beanName)
    如果需要的话，会在实例化对象之前执行
3、MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition(mbd, beanType, beanName)
    在对象实例化完毕 初始化之前执行
4、InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)
    在bean创建完毕初始化之前执行
5、InstantiationAwareBeanPostProcessor.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName)
    在bean的property属性注入完毕 向bean中设置属性之前执行
6、BeanPostProcessor.postProcessBeforeInitialization(result, beanName)
    在bean初始化（自定义init或者是实现了InitializingBean.afterPropertiesSet()）之前执行
7、BeanPostProcessor.postProcessAfterInitialization(result, beanName)
    在bean初始化（自定义init或者是实现了InitializingBean.afterPropertiesSet()）之后执行
8、其中DestructionAwareBeanPostProcessor方法的postProcessBeforeDestruction(Object bean, String beanName)会在销毁对象前执行


DestructionAwareBeanPostProcessor 中的requiresDestruction(Object bean)是用来判断是否属于当前processor处理的bean
SmartInstantiationAwareBeanPostProcessor中的predictBeanType(Class<?> beanClass, String beanName)是用来预判类型的
SmartInstantiationAwareBeanPostProcessor.getEarlyBeanReference(exposedObject, beanName)
    这个方法仅仅是在这一步是作为一个ObjectFactory封装起来放到singletonFactories中的，
    仅在并发情况下 刚好在当前对象设置进去，而另一个bean创建需要getBean获取时才会立即执行
    因此这一步的顺序是不一定的，有可能永远不会执行（无并发循坏依赖对象创建的场景）
    可能在3之后对象实例化完毕执行addSingleton(beanName, singletonObject);之前执行到
因此这三个方法没有严格的顺序意义

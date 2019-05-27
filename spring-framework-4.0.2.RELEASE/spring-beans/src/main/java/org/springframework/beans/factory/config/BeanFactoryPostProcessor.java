/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * Allows for custom modification of an application context's bean definitions,
 * adapting the bean property values of the context's underlying bean factory.
 *
 * <p>Application contexts can auto-detect BeanFactoryPostProcessor beans in
 * their bean definitions and apply them before any other beans get created.
 *
 * <p>Useful for custom config files targeted at system administrators that
 * override bean properties configured in the application context.
 *
 * <p>See PropertyResourceConfigurer and its concrete implementations
 * for out-of-the-box solutions that address such configuration needs.
 *
 * <p>A BeanFactoryPostProcessor may interact with and modify bean
 * definitions, but never bean instances. Doing so may cause premature bean
 * instantiation, violating the container and causing unintended side-effects.
 * If bean instance interaction is required, consider implementing
 * {@link BeanPostProcessor} instead.
 *
 * @author Juergen Hoeller
 * @since 06.07.2003
 * @see BeanPostProcessor
 * @see PropertyResourceConfigurer
 */
public interface BeanFactoryPostProcessor {

	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for overriding or adding
	 * properties even to eager-initializing beans.
	 * @param beanFactory the bean factory used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	//BeanFactoryPostProcessor还有一个子接口为org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
	//
	//BeanDefinitionRegistryPostProcessor拓展了一个方法为void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
	//在对象工厂BeanFactory创建完毕而且正常的BeanDefinition都已经加载完毕而且尚未初始化时调用。用来修改BeanDefinitionRegistry
	//
	//顺序：
	//1.直接注册到AbstractApplicationContext中的beanFactoryPostProcessors且类型为BeanDefinitionRegistryPostProcessor
	//    执行BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
	//2.被BeanFactory通过AbstractRefreshableApplicationContext创建BeanFactory时的loadBeanDefinition加载的（或者第一步修改registry注册进去的）类型为
	//    BeanDefinitionRegistryPostProcessor的而且实现了PriorityOrdered接口的BeanDefinition。然后根据getOrder()的值通过排序器
	//    ((DefaultListableBeanFactory) beanFactory).getDependencyComparator()如果不存在则用OrderComparator.INSTANCE。
	//    调用postProcessor.postProcessBeanDefinitionRegistry(registry);
	//3.被BeanFactory通过AbstractRefreshableApplicationContext创建BeanFactory时的loadBeanDefinition加载的（或者第一步、第二步修改registry注册进去的）类型为
	//    BeanDefinitionRegistryPostProcessor的而且实现了Ordered接口的BeanDefinition,然后根据getOrder()的值通过排序器
	//    ((DefaultListableBeanFactory) beanFactory).getDependencyComparator()如果不存在则用OrderComparator.INSTANCE。
	//    调用postProcessor.postProcessBeanDefinitionRegistry(registry);
	//    注意：PriorityOrdered是Ordered的子接口，因此这里就算第二部注册了一些实现了PriorityOrdered的BeanDefinitionRegistryPostProcessor，
	//    仅仅会根据getOrder()值进行排序执行的。
	//4.到这可能会由3步骤产生新的BeanDefinitionRegistryPostProcessor类型的BeanDefinition，而且每一次调用都可能会产生新的该类型BeanDefinition
	//    这里直接循环（+ 排序）调用postProcessor.postProcessBeanDefinitionRegistry(registry);，
	//    直至没有需要处理的BeanDefinitionRegistryPostProcessor类型的为止。
	//5.执行上面获取的所有BeanPostProcessor中的postProcessor.postProcessBeanFactory(beanFactory);
	//    虽然BeanDefinitionRegistryPostProcessor为BeanPostProcessor的子类，但是上面处理并没有开始执行postProcessor.postProcessBeanFactory(beanFactory);
	//    方法，这一步直接全部挨个先调用BeanDefinitionRegistryPostProcessor的postProcessBeanFactory(beanFactory)后调用
	//    上面从AbstractApplicationContext中获取的类型不为BeanDefinitionRegistryPostProcessor以及1、2、3、4获取的类型为BeanPostProcessor的
	//    postProcessBeanFactory(beanFactory)回调。
	//6.在第四步循环处理的是为子类型BeanDefinitionRegistryPostProcessor，因此会产生很多新的BeanPostProcessor这里统一处理有类型为BeanFactoryPostProcessor的
	//   将所有类型为BeanPostProcessor的根据实现了1、ProrityOrdered接口的与实现2、Ordered接口的以及3、未实现排序接口的分为三组
	//   然后排序依次执行

	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}

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
 * Factory hook that allows for custom modification of new bean instances,
 * e.g. checking for marker interfaces or wrapping them with proxies.
 *
 * <p>ApplicationContexts can autodetect BeanPostProcessor beans in their
 * bean definitions and apply them to any beans subsequently created.
 * Plain bean factories allow for programmatic registration of post-processors,
 * applying to all beans created through this factory.
 *
 * <p>Typically, post-processors that populate beans via marker interfaces
 * or the like will implement {@link #postProcessBeforeInitialization},
 * while post-processors that wrap beans with proxies will normally
 * implement {@link #postProcessAfterInitialization}.
 *
 * @author Juergen Hoeller
 * @since 10.10.2003
 * @see InstantiationAwareBeanPostProcessor
 * @see DestructionAwareBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 */
//关于通过对象工厂BeanFactory创建对象前后的回调处理
public interface BeanPostProcessor {

	//1.在执行registerBeanPostProcessors(beanFactory);之前直接注册到BeanFactory中的顺序不变
	//2.BeanPostProcessorChecker紧随此前直接注册到BeanFacotry中BeanPostProcessor
	//3.实现了PriorityOrdered接口的BeanPostProcessor (根据getOrder()排序)
	//4.实现了Ordered接口的BeanPostProcessor（根据getOrder()排序）
	//5.未实现关于Order接口的普通BeanPostProcessor
	//6.所有类型为BeanPostProcessor的子类型的MergedBeanDefinitionPostProcessor重新注册，使它们的执行顺序在BeanPostProcessor直接类型的后面
	//7.ApplicationListenerDetector这个BeanPostProcessor放到最最后面
	//


	/**
	 * Apply this BeanPostProcessor to the given new bean instance <i>before</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * @param bean the new bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance to use, either the original or a wrapped one; if
	 * {@code null}, no subsequent BeanPostProcessors will be invoked
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException;

	/**
	 * Apply this BeanPostProcessor to the given new bean instance <i>after</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * <p>In case of a FactoryBean, this callback will be invoked for both the FactoryBean
	 * instance and the objects created by the FactoryBean (as of Spring 2.0). The
	 * post-processor can decide whether to apply to either the FactoryBean or created
	 * objects or both through corresponding {@code bean instanceof FactoryBean} checks.
	 * <p>This callback will also be invoked after a short-circuiting triggered by a
	 * {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation} method,
	 * in contrast to all other BeanPostProcessor callbacks.
	 * @param bean the new bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance to use, either the original or a wrapped one; if
	 * {@code null}, no subsequent BeanPostProcessors will be invoked
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.FactoryBean
	 */
	Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException;
//1、InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation(beanClass, beanName)
//    该方法在创建对象之前会先掉用，如果有返回实例则直接使用不会去走下面创建对象的逻辑，并在之后执行
//        BeanPostProcessor.postProcessAfterInitialization(result, beanName)
//2、SmartInstantiationAwareBeanPostProcessor.determineCandidateConstructors(beanClass, beanName)
//    如果需要的话，会在实例化对象之前执行
//3、MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition(mbd, beanType, beanName)
//    在对象实例化完毕 实例化之前执行
//4、InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)
//    在bean创建完毕实例化之前执行
//5、InstantiationAwareBeanPostProcessor.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName)
//    在bean的property属性注入完毕 向bean中设置属性之前执行
//6、BeanPostProcessor.postProcessBeforeInitialization(result, beanName)
//    在bean初始化（自定义init或者是实现了InitializingBean.afterPropertiesSet()）之前执行
//7、BeanPostProcessor.postProcessAfterInitialization(result, beanName)
//    在bean初始化（自定义init或者是实现了InitializingBean.afterPropertiesSet()）之后执行
//8、其中DestructionAwareBeanPostProcessor方法的postProcessBeforeDestruction(Object bean, String beanName)会在销毁对象前执行
//
//
//DestructionAwareBeanPostProcessor 中的requiresDestruction(Object bean)是用来判断是否属于当前processor处理的bean
//SmartInstantiationAwareBeanPostProcessor中的predictBeanType(Class<?> beanClass, String beanName)是用来预判类型的
//SmartInstantiationAwareBeanPostProcessor.getEarlyBeanReference(exposedObject, beanName)
//    这个方法仅仅是在这一步是作为一个ObjectFactory封装起来放到singletonFactories中的，
//    仅在并发情况下 刚好在当前对象设置进去，而另一个bean创建需要getBean获取时才会立即执行
//    因此这一步的顺序是不一定的，有可能永远不会执行（无并发循坏依赖对象创建的场景）
//    可能在3之后对象实例化完毕执行addSingleton(beanName, singletonObject);之前执行到
//因此这三个方法没有严格的顺序意义

}

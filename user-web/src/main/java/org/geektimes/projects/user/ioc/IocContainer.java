package org.geektimes.projects.user.ioc;

import org.geektimes.web.mvc.ioc.Container;

/**
 * @author xiehuang 11108901
 * @create 2021/3/17 18:05
 */
public class IocContainer implements Container {
    @Override
    public Object getObject(String name) {
        return null;
    }

    @Override
    public Container getParentContainer() {
        return null;
    }

    @Override
    public void setParentContainer(Container container) {

    }
}

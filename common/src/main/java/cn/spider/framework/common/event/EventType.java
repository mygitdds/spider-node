package cn.spider.framework.common.event;

import cn.spider.framework.common.role.EventTypeRole;
import cn.spider.framework.common.role.SystemRole;

public enum EventType {
    START_FLOW_EXAMPLE(SystemRole.FLOW_EXAMPLE,"start_flow_example","启动新流程",EventTypeRole.BUSINESS),

    END_FLOW_EXAMPLE(SystemRole.FLOW_EXAMPLE,"end_flow_example","流程执行结束",EventTypeRole.BUSINESS),

    ELEMENT_START(SystemRole.ELEMENT_EXAMPLE,"start_element_example","开始执行节点",EventTypeRole.BUSINESS),

    ELEMENT_END(SystemRole.ELEMENT_EXAMPLE,"end_element_example","执行节点结束",EventTypeRole.BUSINESS),

    START_TRANSACTION(SystemRole.TRANSACTION,"run_transaction","开始执行事务",EventTypeRole.BUSINESS),

    END_TRANSACTION(SystemRole.TRANSACTION,"end_transaction","事务执行结束",EventTypeRole.BUSINESS),

    //generate
    LEADER_GENERATE(SystemRole.CONTROLLER,"leader_generate","通知follower,leader已经被创建了",EventTypeRole.SYSTEM),
    //death
    FOLLOWER_DEATH(SystemRole.CONTROLLER,"leader_generate","通知集群所有节点,某节点挂了",EventTypeRole.SYSTEM),

    ;
    private SystemRole role;

    private String name;

    private String desc;

    private EventTypeRole eventTypeRole;

    EventType(SystemRole role, String name, String desc,EventTypeRole eventTypeRole) {
        this.role = role;
        this.name = name;
        this.desc = desc;
        this.eventTypeRole = eventTypeRole;
    }

    public String queryAddr(){
        return this.role.name()+this.name;
    }

    public SystemRole getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public EventTypeRole getEventTypeRole() {
        return eventTypeRole;
    }
}

package com.cotalk.domain.exception;

/**
 * 자기 자신에 대한 액션이 허용되지 않을 때 발생하는 예외.
 * 예: 자기 자신 차단, 자기 자신에게 친구 요청, 자기 자신 신고 등
 *
 * @author seunggu.lee
 */
public class SelfActionNotAllowedException extends DomainException {

    private final String actionType;

    public SelfActionNotAllowedException(String actionType) {
        super(String.format("자기 자신을 %s할 수 없습니다", actionType));
        this.actionType = actionType;
    }

    public String getActionType() {
        return actionType;
    }

    public static SelfActionNotAllowedException block() {
        return new SelfActionNotAllowedException("차단");
    }

    public static SelfActionNotAllowedException friendRequest() {
        return new SelfActionNotAllowedException("친구 요청");
    }

    public static SelfActionNotAllowedException report() {
        return new SelfActionNotAllowedException("신고");
    }
}

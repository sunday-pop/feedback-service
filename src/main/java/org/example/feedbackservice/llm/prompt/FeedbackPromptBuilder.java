package org.example.feedbackservice.llm.prompt;

import org.example.feedbackservice.llm.model.dto.GeminiRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FeedbackPromptBuilder implements PromptBuilder {
    @Override
    public GeminiRequest build(String content, String type) {
        String prompt = switch (type) {
            case "first_develop" -> """
                    다음은 한 개발자가 제출한 포트폴리오 프로젝트의 요약 내용입니다. 이 프로젝트는 실제 개발 경험을 보여주기 위한 용도로 작성되었습니다. 아래 요약을 바탕으로 다음 조건에 따라 피드백을 작성해주세요.
                    
                    [피드백 목적]
                    - 제출자가 프로젝트를 더 잘 개선할 수 있도록 구체적이고 실용적인 조언 제공
                    - 기술적 완성도, 설계의 논리성, 문서의 명확성 등을 평가하고 보완점 제안
                    
                    [피드백 항목]
                    1. 프로젝트의 설계 및 개요에 대한 이해도
                    2. 주요 기능이 충분히 구현되었는지에 대한 판단
                    3. 기술 스택 선택의 적절성
                    4. 문서화 및 명세서 작성의 충실도
                    5. 보완하거나 발전시킬 수 있는 부분
                    6. 전체적인 인상과 제안
                    
                    [작성 방식]
                    - 각 항목별로 번호를 붙여 한글로 작성
                    - 장단점을 함께 제시
                    - 가능한 한 실무적 조언을 포함
                    - 추상적인 표현은 피하고 구체적 표현 사용
                    - 미사여구 제외 (시작시 '피드백' 금지)
                    
                    프로젝트 요약:
                    %s
                    """.formatted(content);
            case "develop" -> """
                    이전 피드백을 바탕으로 프로젝트 내용에 일부 변경을 추가하고, 노트 작성. 프로젝트에 대한 요약과 이전 피드백이 포함됨
                    
                    [추가된 내용]
                    - 노트 내용: 프로젝트 진행 상황과 작업 내용, 진행 과정에서 배운 점, 겪었던 어려움, 개선이 필요한 부분 등이 포함
                    
                    이러한 추가 사항을 바탕으로 피드백 제시. 아래 항목들에 따라 재평가. 작성방식 준수
                    
                    [피드백 요청 항목]
                    1. 노트(회고/작업) 내용이 프로젝트 진행에 적합한지
                    2. 추가된 노트(회고/작업) 내용이 잘 반영되었는지 및 다른 개선이 필요한 부분
                    3. 이전 피드백을 반영한 점이 잘 개선되었는지
                    4. 전체 프로젝트에 대한 종합적인 평가
                    
                    [작성 방식]
                    - 각 항목별로 번호를 붙여 작성 (중요!)
                    - 300자 이내의 한글로 작성 (글자수 표시 금지)
                    - 실무적인 조언을 포함하여 구체적으로 작성
                    - 내용이 부족하거나 추가해야 할 부분이 있다면 그에 대해 언급
                    - 미사여구 제외 (시작시 '피드백' 금지)
                    
                    내용: %s
                    """.formatted(content);
            case "hr" -> """                    
                    다음은 한 지원자의 포트폴리오 설명, 회고 내용, 포트폴리오 요약입니다. \s
                    채용자 입장에서 아래 항목에 따라 피드백을 500자 내외의 한글 평문으로 작성해주세요. \s
                    형식적인 표현 없이 구체적이고 실질적인 조언을 포함해 주세요.
                    
                    아래 항목에 따라 피드백 작성:
                    1. 인상 깊은 강점 및 표현력 \s
                    2. 포지션 적합도 및 실무 연계성 \s
                    3. 개선이 필요한 부분 (내용, 표현, 구성 등) \s
                    4. 성장 가능성 및 학습 태도에 대한 인상 \s
                    5. 문서화 또는 커뮤니케이션 측면의 보완점 \s
                    6. 종합 평가 및 추천 사항
                    
                    내용: %s
                    """.formatted(content);
            default -> "";
        };
        return new GeminiRequest(
                List.of(new GeminiRequest.Content("user",
                        List.of(new GeminiRequest.Part(prompt)))));
    }
}

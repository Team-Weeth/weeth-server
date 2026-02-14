package leets.weeth.domain.penalty.application.usecase;

import jakarta.transaction.Transactional;
import leets.weeth.domain.penalty.application.dto.PenaltyDTO;
import leets.weeth.domain.penalty.application.exception.AutoPenaltyDeleteNotAllowedException;
import leets.weeth.domain.penalty.application.mapper.PenaltyMapper;
import leets.weeth.domain.penalty.domain.entity.Penalty;
import leets.weeth.domain.penalty.domain.entity.enums.PenaltyType;
import leets.weeth.domain.penalty.domain.service.PenaltyDeleteService;
import leets.weeth.domain.penalty.domain.service.PenaltyFindService;
import leets.weeth.domain.penalty.domain.service.PenaltySaveService;
import leets.weeth.domain.penalty.domain.service.PenaltyUpdateService;
import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.UserCardinal;
import leets.weeth.domain.user.domain.service.CardinalGetService;
import leets.weeth.domain.user.domain.service.UserCardinalGetService;
import leets.weeth.domain.user.domain.service.UserGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PenaltyUsecaseImpl implements PenaltyUsecase{

    private static final String AUTO_PENALTY_DESCRIPTION = "누적경고 %d회";

    private final PenaltySaveService penaltySaveService;
    private final PenaltyFindService penaltyFindService;
    private final PenaltyUpdateService penaltyUpdateService;
    private final PenaltyDeleteService penaltyDeleteService;

    private final UserGetService userGetService;

    private final UserCardinalGetService userCardinalGetService;
    private final CardinalGetService cardinalGetService;

    private final PenaltyMapper mapper;

    @Override
    @Transactional
    public void save(PenaltyDTO.Save dto) {
        User user = userGetService.find(dto.userId());
        Cardinal cardinal = userCardinalGetService.getCurrentCardinal(user);

        Penalty penalty = mapper.fromPenaltyDto(dto, user, cardinal);

        penaltySaveService.save(penalty);

        if(penalty.getPenaltyType().equals(PenaltyType.PENALTY)){
            user.incrementPenaltyCount();
        } else if (penalty.getPenaltyType().equals(PenaltyType.WARNING)){
            user.incrementWarningCount();

            Integer warningCount = user.getWarningCount();
            if(warningCount % 2 == 0){
                String penaltyDescription = String.format(AUTO_PENALTY_DESCRIPTION, warningCount);
                Penalty autoPenalty = mapper.toAutoPenalty(penaltyDescription, user, cardinal, PenaltyType.AUTO_PENALTY);
                penaltySaveService.save(autoPenalty);
                user.incrementPenaltyCount();
            }
        }
    }

    @Override
    @Transactional
    public void update(PenaltyDTO.Update dto) {
        Penalty penalty = penaltyFindService.find(dto.penaltyId());
        penaltyUpdateService.update(penalty, dto);

    }

    // Todo: 쿼리 최적화 필요
    @Override
    public List<PenaltyDTO.ResponseAll> findAll(Integer cardinalNumber) {
        List<Cardinal> cardinals = (cardinalNumber == null)
                ? cardinalGetService.findAllCardinalNumberDesc()
                : List.of(cardinalGetService.findByAdminSide(cardinalNumber));

        List<PenaltyDTO.ResponseAll> result = new ArrayList<>();

        for (Cardinal cardinal : cardinals) {
            List<Penalty> penalties = penaltyFindService.findAllByCardinalId(cardinal.getId());

            Map<Long, List<Penalty>> penaltiesByUser = penalties.stream()
                    .collect(Collectors.groupingBy(p -> p.getUser().getId()));

            List<PenaltyDTO.Response> responses = penaltiesByUser.entrySet().stream()
                    .map(entry -> toPenaltyDto(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(PenaltyDTO.Response::userId))
                    .toList();

            result.add(mapper.toResponseAll(cardinal.getCardinalNumber(), responses));
        }
        return result;
    }

    @Override
    public PenaltyDTO.Response find(Long userId) {
        User user = userGetService.find(userId);
        Cardinal currentCardinal = userCardinalGetService.getCurrentCardinal(user);
        List<Penalty> penalties = penaltyFindService.findAllByUserIdAndCardinalId(userId, currentCardinal.getId());

        return toPenaltyDto(userId, penalties);
    }

    @Override
    @Transactional
    public void delete(Long penaltyId) {
        Penalty penalty = penaltyFindService.find(penaltyId);
        if(penalty.getPenaltyType().equals(PenaltyType.AUTO_PENALTY)){
            throw new AutoPenaltyDeleteNotAllowedException();
        }

        User user = penalty.getUser();

        if(penalty.getPenaltyType().equals(PenaltyType.PENALTY)){
            penalty.getUser().decrementPenaltyCount();
        } else if (penalty.getPenaltyType().equals(PenaltyType.WARNING)) {
            if(user.getWarningCount() % 2 == 0){
                Penalty relatedAutoPenalty = penaltyFindService.getRelatedAutoPenalty(penalty);
                if(relatedAutoPenalty != null){
                    penaltyDeleteService.delete(relatedAutoPenalty.getId());
                }
                user.decrementPenaltyCount();
            }
            penalty.getUser().decrementWarningCount();
        }

        penaltyDeleteService.delete(penaltyId);
    }

    private PenaltyDTO.Response toPenaltyDto(Long userId, List<Penalty> penalties) {
        User user = userGetService.find(userId);
        List<UserCardinal> userCardinals = userCardinalGetService.getUserCardinals(user);

        List<PenaltyDTO.Penalties> penaltyDTOs = penalties.stream()
                .map(mapper::toPenalties)
                .toList();

        return mapper.toPenaltyDto(user, penaltyDTOs, userCardinals);
    }

}

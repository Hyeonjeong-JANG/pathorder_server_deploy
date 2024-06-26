package shop.project.pathorderserver.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.project.pathorderserver._core.errors.exception.App400;
import shop.project.pathorderserver._core.errors.exception.App401;
import shop.project.pathorderserver._core.errors.exception.App404;
import shop.project.pathorderserver._core.utils.JwtUtil;
import shop.project.pathorderserver.order.*;
import shop.project.pathorderserver.store.Store;
import shop.project.pathorderserver.store.StoreRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderMenuRepository orderMenuRepository;
    private final OrderMenuOptionRepository orderMenuOptionRepository;
    private final StoreRepository storeRepository;

    @Transactional // 회원 가입
    public UserResponse.JoinDTO createUser(UserRequest.JoinDTO reqDTO) {
        Optional<User> userOp = userRepository.findByUsername(reqDTO.getUsername());
        if (userOp.isPresent()) {
            throw new App400("중복된 유저입니다.");
        }
        User user = new User(reqDTO);
        userRepository.save(user);

        return UserResponse.JoinDTO
                .builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .name(user.getName())
                .tel(user.getTel())
                .email(user.getEmail())
                .build();
    }

    // 로그인 TODO: 암호화
    public UserResponse.LoginDTO getUser(UserRequest.LoginDTO reqDTO) {
        User user = userRepository.findByUsernameAndPassword(reqDTO.getUsername(), reqDTO.getPassword())
                .orElseThrow(() -> new App401("아이디 또는 비밀번호가 틀렸습니다."));
        String jwt = JwtUtil.create(user);

        return new UserResponse.LoginDTO(user, jwt);
    }

    // 회원정보 보기
    public UserResponse.UserDTO getUser(int sessionUserId) {
        User user = userRepository.findById(sessionUserId)
                .orElseThrow(() -> new App404("찾을 수 없는 유저입니다."));

        return UserResponse.UserDTO
                .builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .tel(user.getTel())
                .email(user.getEmail())
                .imgFilename(user.getImgFilename())
                .build();
    }

    @Transactional // 회원정보 수정
    public SessionUser setUser(UserRequest.UpdateDTO reqDTO, int sessionUserId) {
        User user = userRepository.findById(sessionUserId)
                .orElseThrow(() -> new App404("회원 정보를 찾을 수 없습니다."));
        user.update(reqDTO);

        return new SessionUser(user);
    }

    @Transactional // 사진 업로드
    public UserResponse.ImgDTO setImg(UserRequest.ImgDTO reqDTO, int sessionUserId) {
        User user = userRepository.findById(sessionUserId)
                .orElseThrow(() -> new App404("찾을 수 없는 유저입니다."));
        user.setImgFilename(reqDTO.getEncodedImg());

        return new UserResponse.ImgDTO(user.getImgFilename());
    }

    @Transactional // 주문하기
    public UserResponse.OrderDTO createOrder(UserRequest.OrderDTO reqDTO) {
        User customer // 유저 번호로 유저 조회
                = userRepository.findById(reqDTO.getCustomerId())
                .orElseThrow(() -> new App404("찾을 수 없는 유저입니다."));
        Store store // 매장 번호로 업주 조회
                = storeRepository.findById(reqDTO.getStoreId())
                .orElseThrow(() -> new App404("찾을 수 없는 매장 번호입니다."));
        Order order // 주문 생성 TODO: status 기본 값 'null'
                = new Order(reqDTO, customer, store);

        List<OrderMenu> orderMenus = new ArrayList<>(); // 1. 응답할 주문 메뉴 리스트 생성
        for (int om = 0; om < reqDTO.getOrderMenuList().size(); om++) {
            List<OrderMenuOption> orderMenuOptions = new ArrayList<>(); // 1. 응답할 주문 메뉴 옵션 리스트 생성
            OrderMenu orderMenu = orderMenuRepository // 2. 주문 메뉴 Entity 생성 및 INSERT
                    .save(new OrderMenu(reqDTO.getOrderMenuList().get(om), order));
            for (int omo = 0; omo < reqDTO.getOrderMenuList().get(om).getOrderMenuOptionList().size(); omo++) {
                OrderMenuOption orderMenuOption = orderMenuOptionRepository // 2. 주문 메뉴 옵션 Entity 생성 및 INSERT
                        .save(new OrderMenuOption(reqDTO.getOrderMenuList().get(om).getOrderMenuOptionList().get(omo), order, orderMenu));
                // orderMenu.updateTotalPrice(orderMenuOption.getPrice()); // 3. 주문 메뉴 옵션 금액 추가 (옵션)
                // order.updateTotalPrice(orderMenuOption.getPrice()); // 3. 주문 총액 업데이트 (메뉴 옵션)
                orderMenuOptions.add(orderMenuOption); // 4. 주문 메뉴 옵션 컬랙션 생성 (주문 + 메뉴 옵션)
            }
            // orderMenu.updateTotalPrice(orderMenu.getPrice()); // 3. 주문 메뉴 금액 추가 (메뉴)
            // orderMenu.setTotalPrice(orderMenu.getTotalPrice() * orderMenu.getQty()); // 3. 주문 메뉴 총액 업데이트 (갯수 처리)
            // order.updateTotalPrice(orderMenu.getTotalPrice()); // 3. 주문 총액 업데이트 (메뉴) // 9000 + 3500
            orderMenu.setOrderMenuOptions(orderMenuOptions); // 4. 주문 메뉴 + 주문 메뉴 옵션 컬랙션
            orderMenus.add(orderMenu); // 4. 주문 메뉴 컬랙션 생성 (주문 + 메뉴)
        }
        for (OrderMenu orderMenu : orderMenus) {
            orderMenu.updateTotalPrice();
        }
        order.setOrderMenus(orderMenus); // 5. 주문 + 주문 메뉴 컬랙션
        order.updateTotalPrice();

        orderRepository.save(order); // 6. 주문 Entity INSERT

        return new UserResponse.OrderDTO(order); // 7. 결과 return
    }

    // 주문내역 목록보기 (손님)
    public UserResponse.OrderListDTO getOrderList(int userId) {
        List<Order> orders = orderRepository.findAllByUserId(userId)
                .orElseThrow(() -> new App404("찾을 수 없는 주문입니다."));

        return new UserResponse.OrderListDTO(orders);
    }

    @Transactional(readOnly = true) // 주문내역 상세보기 (손님)
    public UserResponse.OrderDetailDTO getOrderDetail(int orderId) {
        Order order // 단일 주문 내역 조회
                = orderRepository.findById(orderId)
                .orElseThrow(() -> new App404("찾을 수 없는 주문입니다."));
        List<OrderMenu> orderMenus // 주문 내역의 메뉴 목록.
                = orderMenuRepository.findAllByOrderId(orderId)
                .orElseThrow(() -> new App404("찾을 수 없는 주문입니다."));
        /* 양방향 매핑으로 변경.
        List<Integer> orderMenuIdList; // 34, 35, 36
        orderMenuIdList // 주문 메뉴별 Id 추출
                = orderMenus.stream().map(orderMenu -> orderMenu.getId()).toList();
        for (int orderMenuId : orderMenuIdList) {
            List<OrderOption> orderOptions // 주문 메뉴별 옵션 목록 조회
                    = orderMenuRepository.findAllOptionByMenuId(orderMenuId)
                            .orElse(null); // 없으면 null
            orderMenus.get(orderMenuId).setOrderOption(orderOptions);
        }
        */
        return new UserResponse.OrderDetailDTO(order, orderMenus);
    }
}

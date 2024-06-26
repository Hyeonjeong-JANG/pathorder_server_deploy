package shop.project.pathorderserver.store;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import shop.project.pathorderserver._core.utils.ApiUtil;
import shop.project.pathorderserver.user.SessionUser;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class StoreController {
    private final StoreService storeService;
    private final HttpSession session;

    @GetMapping("/api/stores")
    public ResponseEntity<?> storeList() { // 매장 목록보기
        SessionUser sessionUser = (SessionUser) session.getAttribute("sessionUser");
        List<StoreResponse.StoreListDTO> respDTO = storeService.getStoreList(sessionUser.getId(), sessionUser.getLatitude(), sessionUser.getLongitude());

        return ResponseEntity.ok(new ApiUtil<>(respDTO));
    }

    @GetMapping("/api/stores/{storeId}") // 매장 상세보기
    public ResponseEntity<?> storeInfo(@PathVariable("storeId") int storeId) {
        SessionUser sessionUser = (SessionUser) session.getAttribute("sessionUser");
        double customerLatitude = sessionUser.getLatitude();
        double customerLongitude = sessionUser.getLongitude();

        StoreResponse.StoreInfoDTO respDTO = storeService.getStoreInfo(sessionUser.getId(), storeId, customerLatitude, customerLongitude);
        return ResponseEntity.ok(new ApiUtil<>(respDTO));
    }

    @GetMapping("/api/stores/{storeId}/biz-info") // 매장 상세보기 - 사업자 정보 TODO: 상세보기랑 합치기?
    public ResponseEntity<?> storeBizInfo(@PathVariable("storeId") int storeId) {
        StoreResponse.StoreBizInfoDTO respDTO = storeService.getStoreBizInfo(storeId);

        return ResponseEntity.ok(new ApiUtil<>(respDTO));
    }

    @GetMapping("/api/stores/{storeId}/menus") // 매장 메뉴보기
    public ResponseEntity<?> storeMenuList(@PathVariable("storeId") int storeId) {
        StoreResponse.StoreMenuListDTO respDTO = storeService.getStoreMenuList(storeId);

        return ResponseEntity.ok(new ApiUtil<>(respDTO));
    }

    @GetMapping("/api/stores/{storeId}/menus/{menuId}") // 매장 메뉴 옵션보기
    public ResponseEntity<?> storeMenuDetail(@PathVariable("storeId") int storeId, @PathVariable("menuId") int menuId) {
        StoreResponse.StoreMenuOptionDTO respDTO = storeService.getStoreMenuDetail(storeId, menuId);

        return ResponseEntity.ok(new ApiUtil<>(respDTO));
    }
}

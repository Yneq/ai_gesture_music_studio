package com.vance.gesturemusicstudio.service.impl;

import com.vance.gesturemusicstudio.dao.FavoriteLayoutDao;
import com.vance.gesturemusicstudio.dto.layout.LayoutRequest;
import com.vance.gesturemusicstudio.dto.layout.LayoutResponse;
import com.vance.gesturemusicstudio.model.FavoriteLayout;
import com.vance.gesturemusicstudio.model.User;
import com.vance.gesturemusicstudio.exception.ApiException;
import com.vance.gesturemusicstudio.service.LayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LayoutServiceImpl implements LayoutService {

    private final FavoriteLayoutDao favoriteLayoutDao;

    @Override
    public List<LayoutResponse> listLayouts(User user) {
        return favoriteLayoutDao.findByUserId(user.getId())
                .stream()
                .map(LayoutResponse::from)
                .toList();
    }

    @Override
    public LayoutResponse getLayout(User user, Long id) {
        return LayoutResponse.from(findOwnedLayout(user, id));
    }

    @Override
    @Transactional
    public LayoutResponse createLayout(User user, LayoutRequest request) {
        FavoriteLayout layout = FavoriteLayout.builder()
                .user(user)
                .layoutJson(request.getLayoutJson())
                .build();

        return LayoutResponse.from(favoriteLayoutDao.save(layout));
    }

    @Override
    @Transactional
    public LayoutResponse updateLayout(User user, Long id, LayoutRequest request) {
        FavoriteLayout layout = findOwnedLayout(user, id);
        layout.setLayoutJson(request.getLayoutJson());
        return LayoutResponse.from(favoriteLayoutDao.save(layout));
    }

    @Override
    @Transactional
    public void deleteLayout(User user, Long id) {
        FavoriteLayout layout = findOwnedLayout(user, id);
        favoriteLayoutDao.delete(layout);
    }

    private FavoriteLayout findOwnedLayout(User user, Long id) {
        return favoriteLayoutDao.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到布局"));
    }
}

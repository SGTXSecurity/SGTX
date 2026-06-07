package com.sgtx.global.common;

import com.sgtx.domain.item.entity.ItemEntity;
import com.sgtx.domain.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final ItemRepository itemRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        List<ItemEntity> items = itemRepository.findAll();
        model.addAttribute("items", items);
        return "index";
    }
}
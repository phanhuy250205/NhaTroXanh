package nhatroxanh.com.Nhatroxanh.Model.mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import nhatroxanh.com.Nhatroxanh.Model.Dto.PostDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;

@Component
public class PostMapper {
    
    public PostDTO toDTO(Post post) {
        if (post == null) return null;
        
        PostDTO dto = new PostDTO(
            post.getPostId(),
            post.getTitle(),
            post.getDescription(),
            post.getPrice(),
            post.getArea(),
            post.getView(),
            post.getCreatedAt(),
            post.getApprovalStatus() != null ? post.getApprovalStatus().toString() : null
        );
        
        // Map category
        if (post.getCategory() != null) {
            dto.setCategory(new PostDTO.CategoryDTO(
                post.getCategory().getCategoryId(),
                post.getCategory().getName()
            ));
        }
        
        // Map address
        if (post.getAddress() != null) {
            PostDTO.AddressDTO.WardDTO.DistrictDTO.ProvinceDTO provinceDTO = null;
            PostDTO.AddressDTO.WardDTO.DistrictDTO districtDTO = null;
            PostDTO.AddressDTO.WardDTO wardDTO = null;
            
            if (post.getAddress().getWard() != null) {
                if (post.getAddress().getWard().getDistrict() != null) {
                    if (post.getAddress().getWard().getDistrict().getProvince() != null) {
                        provinceDTO = new PostDTO.AddressDTO.WardDTO.DistrictDTO.ProvinceDTO(
                            post.getAddress().getWard().getDistrict().getProvince().getName()
                        );
                    }
                    districtDTO = new PostDTO.AddressDTO.WardDTO.DistrictDTO(
                        post.getAddress().getWard().getDistrict().getName(),
                        provinceDTO
                    );
                }
                wardDTO = new PostDTO.AddressDTO.WardDTO(
                    post.getAddress().getWard().getName(),
                    districtDTO
                );
            }
            
            dto.setAddress(new PostDTO.AddressDTO(
                post.getAddress().getStreet(),
                wardDTO
            ));
        }
        
        // Map images
        if (post.getImages() != null) {
            List<PostDTO.ImageDTO> imageDTOs = post.getImages().stream()
                .map(image -> new PostDTO.ImageDTO(image.getUrl()))
                .collect(Collectors.toList());
            dto.setImages(imageDTOs);
        }
        
        // Map utilities
        if (post.getUtilities() != null) {
            Set<PostDTO.UtilityDTO> utilityDTOs = post.getUtilities().stream()
                .map(utility -> new PostDTO.UtilityDTO(
                    utility.getUtilityId(),
                    utility.getName()
                ))
                .collect(Collectors.toSet());
            dto.setUtilities(utilityDTOs);
        }
        
        return dto;
    }
    
    public List<PostDTO> toDTOList(List<Post> posts) {
        if (posts == null) return null;
        return posts.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
}

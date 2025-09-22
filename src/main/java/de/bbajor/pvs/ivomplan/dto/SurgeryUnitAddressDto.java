package de.bbajor.pvs.ivomplan.dto;

import de.bbajor.pvs.base.dto.AddressDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class SurgeryUnitAddressDto extends AddressDto {

}

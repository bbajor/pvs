package de.bbajor.pvs.egk.reader;

import java.io.StringReader;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

public class XmlStringReader {

    public static <T> T fromXmlString(String xml, Class<T> clazz) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        try (StringReader reader = new StringReader(xml)) {
            @SuppressWarnings("unchecked")
            T result = (T) unmarshaller.unmarshal(reader);
            return result;
        }
    }
}

package de.bbajor.pvs.ivomdrug.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormRow;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import de.bbajor.pvs.ivomdrug.controller.IvomDrugViewPresenter;
import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;

public class IvomDrugDetailDialog extends Dialog {

    private Binder<IvomDrugDto> binder = new Binder<>();

    public IvomDrugDetailDialog(IvomDrugViewPresenter presenter, IvomDrugDto ivomDrug) {

        boolean isReadOnly = true;
        setWidth("1200px");
        setHeight("1000px");
        setCloseOnEsc(true);

        TextField nr = new TextField("Nr");
        nr.setReadOnly(isReadOnly);
        TextField eingangsnummer = new TextField("Eingangsnummer");
        eingangsnummer.setReadOnly(isReadOnly);
        TextField arzneimittelBezeichnung = new TextField("Arzneimittelbezeichnung");
        arzneimittelBezeichnung.setReadOnly(isReadOnly);
        TextField darreichungsForm = new TextField("Darreichungsform");
        darreichungsForm.setReadOnly(isReadOnly);
        TextField zielgruppe = new TextField("Zielgruppe");
        zielgruppe.setReadOnly(isReadOnly);
        TextField anwendungsArt = new TextField("Anwendungsart");
        anwendungsArt.setReadOnly(isReadOnly);
        TextArea anwendungsGebiete = new TextArea("Anwendungsgebiete");
        anwendungsGebiete.setHeight("80px");
        anwendungsGebiete.setReadOnly(isReadOnly);
        TextField indikationAtc = new TextField("Indikation/ATC");
        indikationAtc.setReadOnly(isReadOnly);
        TextField bescheidDatumZulassung = new TextField("Bescheiddatum der Zulassung");
        bescheidDatumZulassung.setReadOnly(isReadOnly);
        TextField zulassungsStatus = new TextField("Zulassungsstatus");
        zulassungsStatus.setReadOnly(isReadOnly);
        TextField zulassungsRegNrOderKennziffer = new TextField(
                "Zulassungs-/Reg.-Nr. (AMG 1976), Register-Nr. (AMG 1961) oder Kennziffer");
        zulassungsRegNrOderKennziffer.setReadOnly(isReadOnly);
        TextField verkehrsFaehigkeit = new TextField("Verkehrsfähigkeit");
        verkehrsFaehigkeit.setReadOnly(isReadOnly);
        TextField parallelImportInformationen = new TextField("Parallelimportinformationen");
        parallelImportInformationen.setReadOnly(isReadOnly);
        TextField euVerfahrensNummer = new TextField("EU-Verfahrensnummer");
        euVerfahrensNummer.setReadOnly(isReadOnly);
        TextField zulassungsInhaber = new TextField("Zulassungsinhaber");
        zulassungsInhaber.setReadOnly(isReadOnly);
        TextField herstellerEndFreigabe = new TextField("Hersteller/Endfreigabe");
        herstellerEndFreigabe.setReadOnly(isReadOnly);
        TextField vertreiber = new TextField("Vertreiber");
        vertreiber.setReadOnly(isReadOnly);
        TextField oertlicherVertreter = new TextField("Örtlicher Vertreter");
        oertlicherVertreter.setReadOnly(isReadOnly);
        TextField wirkstoffe = new TextField("Wirkstoffe");
        wirkstoffe.setReadOnly(isReadOnly);
        TextArea packungsGroessenGruppeVerkaufsabgrenzung = new TextArea("Packungsgrößen-Gruppe/Verkaufsabgrenzung");
        packungsGroessenGruppeVerkaufsabgrenzung.setHeight("160px");
        packungsGroessenGruppeVerkaufsabgrenzung.setReadOnly(isReadOnly);
        TextArea amKlassifikationen = new TextArea("AM-Klassifikationen");
        amKlassifikationen.setHeight("160px");
        amKlassifikationen.setReadOnly(isReadOnly);
        Checkbox favourite = new Checkbox("Favorit");
        FormLayout detailLayout = new FormLayout();
        FormRow firstRow = new FormRow();
        firstRow.add(nr, eingangsnummer, zielgruppe, anwendungsArt);
        FormRow secondRow = new FormRow();
        secondRow.add(arzneimittelBezeichnung, 2);
        secondRow.add(anwendungsArt);
        secondRow.add(darreichungsForm);
        FormRow thirdRow = new FormRow();
        thirdRow.add(anwendungsGebiete, 5);
        FormRow fourthRow = new FormRow();
        fourthRow.add(indikationAtc, zulassungsRegNrOderKennziffer, euVerfahrensNummer);
        detailLayout.add(firstRow, secondRow, thirdRow, fourthRow, bescheidDatumZulassung, zulassungsStatus,
                verkehrsFaehigkeit, parallelImportInformationen,
                zulassungsInhaber, herstellerEndFreigabe, vertreiber, oertlicherVertreter,
                wirkstoffe, packungsGroessenGruppeVerkaufsabgrenzung, amKlassifikationen, favourite);
        detailLayout.setSizeFull();
        Scroller scroller = new Scroller(detailLayout);
        add(scroller);

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        HorizontalLayout dummyLayout = new HorizontalLayout();
        dummyLayout.setSizeFull();
        buttonBar.add(dummyLayout);
        Button saveButton = new Button("Ok");
        saveButton.addClickListener(event -> {
            presenter.save(binder.getBean());
            close();
        });
        Button cancelButton = new Button("Abbrechen");
        cancelButton.addClickListener(event -> {
            close();
        });
        buttonBar.add(saveButton, cancelButton);

        add(buttonBar);

        binder.forField(nr).bind(IvomDrugDto::getZulassungsNr, IvomDrugDto::setZulassungsNr);
        binder.forField(eingangsnummer).bind(IvomDrugDto::getEingangsnummer, IvomDrugDto::setEingangsnummer);
        binder.forField(arzneimittelBezeichnung).bind(IvomDrugDto::getArzneimittelbezeichnung,
                IvomDrugDto::setArzneimittelbezeichnung);
        binder.forField(darreichungsForm).bind(IvomDrugDto::getDarreichungsform, IvomDrugDto::setDarreichungsform);
        binder.forField(zielgruppe).bind(IvomDrugDto::getZielgruppe, IvomDrugDto::setZielgruppe);
        binder.forField(anwendungsArt).bind(IvomDrugDto::getAnwendungsart, IvomDrugDto::setAnwendungsart);
        binder.forField(anwendungsGebiete).bind(IvomDrugDto::getAnwendungsgebiete, IvomDrugDto::setAnwendungsgebiete);
        binder.forField(indikationAtc).bind(IvomDrugDto::getIndikationAtc, IvomDrugDto::setIndikationAtc);
        binder.forField(bescheidDatumZulassung).bind(IvomDrugDto::getBescheiddatumZulassung,
                IvomDrugDto::setBescheiddatumZulassung);
        binder.forField(zulassungsStatus).bind(IvomDrugDto::getZulassungsstatus, IvomDrugDto::setZulassungsstatus);
        binder.forField(zulassungsRegNrOderKennziffer).bind(IvomDrugDto::getZulassungsRegNrOderKennziffer,
                IvomDrugDto::setZulassungsRegNrOderKennziffer);
        binder.forField(verkehrsFaehigkeit).bind(IvomDrugDto::getVerkehrsfaehigkeit,
                IvomDrugDto::setVerkehrsfaehigkeit);
        binder.forField(parallelImportInformationen).bind(IvomDrugDto::getParallelimportinformationen,
                IvomDrugDto::setParallelimportinformationen);
        binder.forField(euVerfahrensNummer).bind(IvomDrugDto::getEuVerfahrensnummer,
                IvomDrugDto::setEuVerfahrensnummer);
        binder.forField(zulassungsInhaber).bind(IvomDrugDto::getZulassungsinhaber, IvomDrugDto::setZulassungsinhaber);
        binder.forField(herstellerEndFreigabe).bind(IvomDrugDto::getHerstellerEndfreigabe,
                IvomDrugDto::setHerstellerEndfreigabe);
        binder.forField(vertreiber).bind(IvomDrugDto::getVertreiber, IvomDrugDto::setVertreiber);
        binder.forField(oertlicherVertreter).bind(IvomDrugDto::getOertlicherVertreter,
                IvomDrugDto::setOertlicherVertreter);
        binder.forField(wirkstoffe).bind(IvomDrugDto::getWirkstoffe, IvomDrugDto::setWirkstoffe);
        binder.forField(packungsGroessenGruppeVerkaufsabgrenzung).bind(IvomDrugDto::getPackungsgroessenGruppe,
                IvomDrugDto::setPackungsgroessenGruppe);
        binder.forField(amKlassifikationen).bind(IvomDrugDto::getAmKlassifikationen,
                IvomDrugDto::setAmKlassifikationen);
        binder.forField(favourite).bind(IvomDrugDto::isFavourite, IvomDrugDto::setFavourite);

        binder.setBean(ivomDrug);
    }

}

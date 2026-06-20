package org.example.reproductor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import recursos.ListaDoble;
import recursos.ListaOrdenada;
import recursos.NodoDoble;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReproductorController {

    @FXML
    private TableView<Song> tablaCanciones;

    @FXML
    private TableColumn<Song, String> colNombre;

    @FXML
    private TableColumn<Song, String> colArtista;

    @FXML
    private TableColumn<Song, String> colAnio;

    @FXML
    private TableColumn<Song, String> colAlbum;

    @FXML
    private TableColumn<Song, String> colDuracion;


    @FXML
    private Label lblCancion;

    @FXML
    private Label lblArtista;

    @FXML
    private Label lblTiempoActual;

    @FXML
    private Label lblDuracion;

    @FXML
    private Slider sliderTiempo;

    @FXML
    private Button btnPlayPause;

    @FXML
    private CheckBox checkLoop;

    @FXML
    private CheckBox checkRandom;

    @FXML
    private Slider sliderVolumen;

    @FXML
    private ImageView imgCover;

    private ListaDoble canciones = new ListaDoble();

    private final ObservableList<Song> observableCanciones = FXCollections.observableArrayList();

    private MediaPlayer mediaPlayer;

    private NodoDoble nodoActual = null;

    private final List<NodoDoble> randomPendientes = new ArrayList<>();

    private boolean pausado = false;

    private String criterioActual = "nombre";

    @FXML
    public void ordenarPorNombre() { ordenar("nombre"); }

    @FXML
    public void ordenarPorArtista() { ordenar("artista"); }

    @FXML
    public void ordenarPorAnio() { ordenar("anio"); }

    @FXML
    public void initialize() {
        tablaCanciones.setItems(observableCanciones);

        // Habilitar selección múltiple para eliminar varias canciones a la vez
        tablaCanciones.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        colArtista.setCellValueFactory(new PropertyValueFactory<>("artista"));

        colAnio.setCellValueFactory(new PropertyValueFactory<>("anio"));

        colAlbum.setCellValueFactory(new PropertyValueFactory<>("album"));

        colDuracion.setCellValueFactory(new PropertyValueFactory<>("duracion"));

        tablaCanciones.setRowFactory(tv -> new TableRow<Song>() {
            @Override
            protected void updateItem(Song item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    getStyleClass().remove("playing-row");
                } else {
                    // Ahora validamos por la identidad de la canción dentro del nodo actual
                    if (nodoActual != null && nodoActual.getNodoInfo() == item) {
                        if (!getStyleClass().contains("playing-row")) {
                            getStyleClass().add("playing-row");
                        }
                    } else {
                        getStyleClass().remove("playing-row");
                    }
                }
            }
        });

        sliderVolumen.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue() / 100.0);
            }
        });

        tablaCanciones.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song song = tablaCanciones.getSelectionModel().getSelectedItem();
                if (song != null) {
                    nodoActual = canciones.buscarNodo(song);
                    reproducirActual();
                }
            }
        });
    }

    @FXML
    public void agregarCanciones() {
        FileChooser chooser = new FileChooser();

        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3", "*.mp3"));

        List<File> archivos = chooser.showOpenMultipleDialog(null);

        if (archivos == null) return;

        for (File file : archivos) {
            try {
                AudioFile audioFile = AudioFileIO.read(file);

                String titulo = audioFile.getTag().getFirst(FieldKey.TITLE);

                String artista = audioFile.getTag().getFirst(FieldKey.ARTIST);

                String album = audioFile.getTag().getFirst(FieldKey.ALBUM);

                String anio = audioFile.getTag().getFirst(FieldKey.YEAR);

                int segundos = audioFile.getAudioHeader().getTrackLength();

                String duracion = String.format("%02d:%02d", segundos / 60, segundos % 60);

                if (titulo.isEmpty()) titulo = file.getName().replace(".mp3", "");

                if (artista.isEmpty()) artista = "Desconocido";

                if (album.isEmpty()) album = "Desconocido";

                if (anio.isEmpty()) anio = "----";

                Image portada;
                try {
                    if (audioFile.getTag().getFirstArtwork() != null) {
                        byte[] imgBytes = audioFile.getTag().getFirstArtwork().getBinaryData();
                        portada = new Image(new java.io.ByteArrayInputStream(imgBytes));
                    } else {
                        portada = new Image(getClass().getResourceAsStream("/default_cover.png"));
                    }
                } catch (Exception e) {
                    portada = new Image(getClass().getResourceAsStream("/default_cover.png"));
                }

                canciones.add(new Song(titulo, artista, album, anio, duracion, file.getAbsolutePath(), portada));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        randomPendientes.clear();
        ordenar(criterioActual);
    }

    @FXML
    public void eliminarCancion() {
        List<Song> seleccionadas = new ArrayList<>(tablaCanciones.getSelectionModel().getSelectedItems());

        if (!seleccionadas.isEmpty()) {
            for (Song song : seleccionadas) {
                if (nodoActual != null && nodoActual.getNodoInfo().equals(song)) { //Si se desea eliminar la cancion que se esta reproduciendo
                    stop();
                }
                canciones.remove(song);
                observableCanciones.remove(song);
            }
            randomPendientes.clear();
            tablaCanciones.refresh();
        }
    }

    @FXML
    public void limpiarLista() {
        stop();
        canciones.clear();
        observableCanciones.clear();
        randomPendientes.clear();
        tablaCanciones.refresh();
    }

    @FXML
    public void togglePlayPause() {
        if (mediaPlayer == null) {
            if (!canciones.isEmpty()) {
                if (nodoActual == null) {
                    nodoActual = canciones.getCabeza();
                }
                reproducirActual();
            }
            return;
        }

        if (pausado) {
            mediaPlayer.play();
            btnPlayPause.setText("⏸");
            pausado = false;

        } else {
            mediaPlayer.pause();
            btnPlayPause.setText("▶");
            pausado = true;
        }
    }

    private void reproducirActual() {
        if (nodoActual != null) {
            reproducir((Song) nodoActual.getNodoInfo());
        }
    }

    private void reproducir(Song song) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        tablaCanciones.refresh();

        Media media = new Media(new File(song.getRuta()).toURI().toString());

        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.play();

        pausado = false;

        btnPlayPause.setText("⏸");

        lblCancion.setText(song.getNombre());

        lblArtista.setText(song.getArtista());

        imgCover.setImage(song.getPortada());

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            sliderTiempo.setValue(newTime.toSeconds());

            lblTiempoActual.setText(formatTime(newTime));
        });

        mediaPlayer.setOnReady(() -> {

            Duration total = mediaPlayer.getTotalDuration();

            sliderTiempo.setMax(total.toSeconds());

            lblDuracion.setText(formatTime(total));
        });

        mediaPlayer.setVolume(sliderVolumen.getValue() / 100.0);

        sliderVolumen.setOnScroll(event -> {

            double delta = event.getDeltaY();

            sliderVolumen.setValue(sliderVolumen.getValue() + delta / 10);
        });

        sliderTiempo.setOnMousePressed(event -> {

            double porcentaje = event.getX() / sliderTiempo.getWidth();

            double tiempo = porcentaje * sliderTiempo.getMax();

            sliderTiempo.setValue(tiempo);

            mediaPlayer.seek(Duration.seconds(tiempo));
        });

        sliderTiempo.setOnMouseDragged(event -> {

            double porcentaje = event.getX() / sliderTiempo.getWidth();

            double tiempo = porcentaje * sliderTiempo.getMax();

            sliderTiempo.setValue(tiempo);

            mediaPlayer.seek(Duration.seconds(tiempo));
        });

        mediaPlayer.setOnEndOfMedia(this::manejarFinCancion);
    }

    private void manejarFinCancion() {
        if (checkRandom.isSelected()) {
            reproducirRandom();
            return;
        }

        if (nodoActual != null && nodoActual.getNextNodo() != null) {

            nodoActual = nodoActual.getNextNodo();

            reproducirActual();

        } else {

            if (checkLoop.isSelected()) {

                nodoActual = canciones.getCabeza();

                reproducirActual();

            } else {
                stop();
            }
        }
    }

    @FXML
    public void siguiente() {
        if (canciones.isEmpty()) return;

        if (checkRandom.isSelected()) {

            reproducirRandom();
            return;
        }
        if (nodoActual != null && nodoActual.getNextNodo() != null) {

            nodoActual = nodoActual.getNextNodo();

            reproducirActual();
        } else {
            if (checkLoop.isSelected()) {

                nodoActual = canciones.getCabeza();

                reproducirActual();
            } else {
                stop();
            }
        }
    }

    @FXML
    public void anterior() {
        if (canciones.isEmpty()) return;

        if (nodoActual != null && nodoActual.getPrevNodo() != null) {

            nodoActual = nodoActual.getPrevNodo();

            reproducirActual();
        } else {
            if (checkLoop.isSelected()) {

                nodoActual = canciones.getCola();

                reproducirActual();
            } else {

                nodoActual = canciones.getCabeza();

                reproducirActual();
            }
        }
    }

    private void reproducirRandom() {
        if (randomPendientes.isEmpty()) {
            NodoDoble actual = canciones.getCabeza();

            while (actual != null) {
                randomPendientes.add(actual);
                actual = actual.getNextNodo();
            }
            Collections.shuffle(randomPendientes);
        }

        if (!randomPendientes.isEmpty()) {
            nodoActual = randomPendientes.remove(0);
            reproducirActual();
        }
    }

    private String formatTime(Duration duration) {
        int minutos = (int) duration.toMinutes();
        int segundos = (int) duration.toSeconds() % 60;
        return String.format("%02d:%02d", minutos, segundos);
    }

    private void ordenar(String criterio) {
        this.criterioActual = criterio;
        ListaOrdenada ordenada = new ListaOrdenada();

        for (Object obj : canciones) {
            Song song = (Song) obj;

            if (criterio.equalsIgnoreCase("artista")) {
                ordenada.addOrderedPorArtista(song);

            } else if (criterio.equalsIgnoreCase("anio")) {
                ordenada.addOrderedPorAnio(song);

            } else {
                ordenada.addOrderedPorNombre(song);
            }
        }
        this.canciones = ordenada;

        observableCanciones.clear();

        for (Object obj : canciones) {
            observableCanciones.add((Song) obj);
        }

        if (nodoActual != null) {
            Song cancionSonando = (Song) nodoActual.getNodoInfo();
            nodoActual = canciones.buscarNodo(cancionSonando);
        }
        randomPendientes.clear();

        tablaCanciones.refresh();
    }

    @FXML
    public void stop() {

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer = null;
        }
        pausado = false;

        btnPlayPause.setText("▶");

        nodoActual = null; // Reseteamos el puntero a nulo

        lblCancion.setText("No hay canción");

        lblArtista.setText("...");

        imgCover.setImage(null);

        sliderTiempo.setValue(0);

        lblTiempoActual.setText("00:00");

        tablaCanciones.refresh();
    }
}
